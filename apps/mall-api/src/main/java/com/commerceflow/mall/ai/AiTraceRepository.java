package com.commerceflow.mall.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiTraceRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AiTraceRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void save(AiModels.CustomerServiceAnswer answer, long userId, long productId, long skuId, String questionSummary, String errorCode) {
        try {
            jdbc.update("""
                    INSERT INTO ai_trace(trace_id,user_id,question,question_summary,client_request_id,product_id,sku_id,
                                         provider_name,provider_mode,model_name,answer,answer_status,status,evidence_json,
                                         latency_ms,fallback_used,error_code)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """,
                    answer.traceId(), userId, questionSummary, questionSummary, answer.clientRequestId(), productId, skuId,
                    answer.provider().name(), answer.provider().mode().name(), answer.provider().model(), answer.answer(),
                    answer.answerStatus().name(), answer.answerStatus().name(), objectMapper.writeValueAsString(answer.evidence()),
                    answer.latencyMs(), answer.fallbackUsed(), errorCode);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Java evidence could not be serialized", ex);
        }
    }
}
