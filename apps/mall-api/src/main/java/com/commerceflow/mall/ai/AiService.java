package com.commerceflow.mall.ai;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.catalog.CatalogRepository;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private final CatalogRepository catalog;
    private final JdbcTemplate jdbc;
    private final String baseUrl;

    public AiService(CatalogRepository catalog, JdbcTemplate jdbc, @Value("${commerceflow.ai-service-url:http://127.0.0.1:8000}") String baseUrl) {
        this.catalog = catalog;
        this.jdbc = jdbc;
        this.baseUrl = baseUrl;
    }

    public ApiModels.AiAnswer chat(ApiModels.ProductChatRequest request) {
        String trace=UUID.randomUUID().toString();
        ApiModels.Sku sku = catalog.sku(10001L);
        long productId = catalog.productId(sku.id());
        String facts="{\"question\":"+json(request.question())+",\"productId\":"+productId+",\"productName\":"+json(catalog.productName(sku.id()))+",\"productStatus\":\"ON_SALE\",\"skuId\":"+sku.id()+",\"skuCode\":"+json(sku.skuCode())+",\"color\":"+json(sku.color())+",\"size\":"+json(sku.size())+",\"availableStock\":"+sku.availableStock()+",\"salePrice\":"+json(sku.salePrice().toPlainString())+",\"currency\":\"CNY\",\"knowledgeSnippets\":[],\"queriedAt\":\""+java.time.OffsetDateTime.now()+"\"}";
        String answer; String mode="mock"; String status="COMPLETED";
        try {
            var body="{\"question\":"+json(request.question())+",\"traceId\":"+json(trace)+",\"businessFacts\":"+facts+"}";
            HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl+"/v1/product-answer").toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(4000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            connection.getOutputStream().write(payload);
            if (connection.getResponseCode() != 200) throw new IllegalStateException("AI service returned " + connection.getResponseCode());
            String response;
            try (InputStream input = connection.getInputStream()) { response = new String(input.readAllBytes(), StandardCharsets.UTF_8); }
            answer=extract(response,"answer"); mode=extract(response,"providerMode"); if(answer==null) throw new IllegalStateException();
        }
        catch(Exception ex) { answer="Based on current Java business facts, the requested product information is available in Mock mode. The AI service is temporarily unavailable."; status="AI_FALLBACK"; }
        jdbc.update("INSERT INTO ai_trace(trace_id,user_id,question,provider_mode,answer,evidence_json,status) VALUES (?,?,?,?,?,?,?)",trace,request.userId(),request.question(),mode,answer,"[\"java.businessFacts\"]",status);
        return new ApiModels.AiAnswer(trace,answer,mode,status,List.of("java.businessFacts"));
    }
    private String json(String value) { return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\""; }
    private String extract(String json,String key) { var marker="\""+key+"\":\""; int start=json.indexOf(marker); if(start<0)return null; start+=marker.length(); int end=json.indexOf('"',start); return end<0?null:json.substring(start,end); }
}
