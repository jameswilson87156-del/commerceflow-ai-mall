package com.commerceflow.mall.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Bounded, typed Java-to-Python client. It never serializes facts by hand. */
@Component
@ConditionalOnProperty(prefix = "commerceflow.ai.provider", name = "mode", havingValue = "FASTAPI", matchIfMissing = true)
public class HttpCustomerServiceProviderClient implements CustomerServiceProviderClient {
    private final RestClient client;

    public HttpCustomerServiceProviderClient(
            ObjectMapper objectMapper,
            @Value("${commerceflow.ai-service-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${commerceflow.ai-connect-timeout-ms:1500}") int connectTimeoutMs,
            @Value("${commerceflow.ai-read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        ObjectMapper strictMapper = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .messageConverters(converters -> {
                    converters.removeIf(converter -> converter instanceof MappingJackson2HttpMessageConverter);
                    converters.add(new MappingJackson2HttpMessageConverter(strictMapper));
                })
                .build();
    }

    @Override
    public AiModels.PythonCustomerServiceResponse answer(AiModels.PythonCustomerServiceRequest request) {
        try {
            AiModels.PythonCustomerServiceResponse response = client.post()
                    .uri("/internal/ai/customer-service/answer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiModels.PythonCustomerServiceResponse.class);
            if (response == null) {
                throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE, "Python returned an empty response");
            }
            return response;
        } catch (AiProviderClientException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            AiProviderClientException.Kind kind = hasTimeout(ex) ? AiProviderClientException.Kind.TIMEOUT : AiProviderClientException.Kind.UNAVAILABLE;
            throw new AiProviderClientException(kind, "Python service could not be reached", ex);
        } catch (RestClientResponseException ex) {
            throw new AiProviderClientException(AiProviderClientException.Kind.UNAVAILABLE, "Python service returned an error", ex);
        } catch (RestClientException ex) {
            throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE, "Python response could not be decoded", ex);
        }
    }

    private boolean hasTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) return true;
            current = current.getCause();
        }
        return false;
    }
}
