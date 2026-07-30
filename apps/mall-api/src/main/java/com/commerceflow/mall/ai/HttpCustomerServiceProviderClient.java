package com.commerceflow.mall.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Bounded Java-to-Python client. It maps only typed, sanitized provider failures. */
@Component
public class HttpCustomerServiceProviderClient implements CustomerServiceProviderClient {
    private final RestClient client;
    private final ObjectMapper objectMapper;

    public HttpCustomerServiceProviderClient(
            ObjectMapper objectMapper,
            @Value("${commerceflow.ai-service-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${commerceflow.ai-connect-timeout-ms:1500}") int connectTimeoutMs,
            @Value("${commerceflow.ai-read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        ObjectMapper strictMapper = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.objectMapper = strictMapper;
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
            String safeCode = kind == AiProviderClientException.Kind.TIMEOUT ? "JAVA_FASTAPI_TIMEOUT" : "JAVA_FASTAPI_UNAVAILABLE";
            throw new AiProviderClientException(kind, AiProviderClientException.FailureSource.JAVA_TO_FASTAPI, safeCode,
                    "Python service could not be reached", ex);
        } catch (RestClientResponseException ex) {
            throw failureFor(ex);
        } catch (RestClientException ex) {
            throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE,
                    AiProviderClientException.FailureSource.PROVIDER_RESPONSE_VALIDATION, "PROVIDER_INVALID_RESPONSE",
                    "Python response could not be decoded", ex);
        }
    }

    private AiProviderClientException failureFor(RestClientResponseException exception) {
        try {
            JsonNode root = objectMapper.readTree(exception.getResponseBodyAsString());
            String code = root.path("code").asText("");
            return switch (code) {
                case "PROVIDER_CONFIGURATION_ERROR" -> failure(AiProviderClientException.Kind.CONFIGURATION,
                        AiProviderClientException.FailureSource.PROVIDER_CONFIGURATION, code, exception);
                case "PROVIDER_PROTOCOL_UNSUPPORTED", "PROVIDER_MODE_UNSUPPORTED" -> failure(AiProviderClientException.Kind.PROTOCOL_UNSUPPORTED,
                        AiProviderClientException.FailureSource.PROVIDER_CONFIGURATION, code, exception);
                case "REMOTE_PROVIDER_TIMEOUT" -> failure(AiProviderClientException.Kind.TIMEOUT,
                        AiProviderClientException.FailureSource.REMOTE_PROVIDER, code, exception);
                case "REMOTE_PROVIDER_UNAVAILABLE", "REMOTE_PROVIDER_AUTH_REJECTED", "REMOTE_PROVIDER_NOT_FOUND",
                        "REMOTE_PROVIDER_RATE_LIMITED", "REMOTE_PROVIDER_SERVER_ERROR", "REMOTE_PROVIDER_HTTP_ERROR" ->
                        failure(AiProviderClientException.Kind.HTTP_ERROR, AiProviderClientException.FailureSource.REMOTE_PROVIDER, code, exception);
                case "PROVIDER_INVALID_JSON", "PROVIDER_INVALID_SCHEMA", "PROVIDER_INVALID_RESPONSE" ->
                        failure(AiProviderClientException.Kind.INVALID_RESPONSE,
                                AiProviderClientException.FailureSource.PROVIDER_RESPONSE_VALIDATION, code, exception);
                case "PROVIDER_FACT_MISMATCH" -> failure(AiProviderClientException.Kind.FACT_MISMATCH,
                        AiProviderClientException.FailureSource.PROVIDER_RESPONSE_VALIDATION, code, exception);
                default -> failure(AiProviderClientException.Kind.INVALID_RESPONSE,
                        AiProviderClientException.FailureSource.PROVIDER_RESPONSE_VALIDATION, "PROVIDER_INVALID_RESPONSE", exception);
            };
        } catch (Exception ignored) {
            return failure(AiProviderClientException.Kind.INVALID_RESPONSE,
                    AiProviderClientException.FailureSource.PROVIDER_RESPONSE_VALIDATION, "PROVIDER_INVALID_RESPONSE", exception);
        }
    }

    private AiProviderClientException failure(
            AiProviderClientException.Kind kind,
            AiProviderClientException.FailureSource source,
            String safeCode,
            RestClientResponseException cause) {
        return new AiProviderClientException(kind, source, safeCode, "Python service rejected the provider result", cause);
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
