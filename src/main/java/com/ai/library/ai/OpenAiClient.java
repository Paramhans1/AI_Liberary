package com.ai.library.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "ai.openai", name = "apiKey")
public class OpenAiClient {
    @Value("${ai.openai.apiKey}")
    private String apiKey;

    @Value("${ai.openai.endpoint}")
    private String endpoint;

    @Value("${ai.openai.model:text-embedding-3-small}")
    private String embeddingModel;

    private final RestTemplate rest = new RestTemplate();
    private final MeterRegistry meterRegistry;

    public OpenAiClient(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // No-arg constructor for tests or environments where MeterRegistry isn't provided
    public OpenAiClient() {
        this.meterRegistry = new SimpleMeterRegistry();
    }

    public double[] generateEmbedding(String text) {
        Timer.Sample sample = Timer.start(meterRegistry);
        // Simple wrapper around OpenAI embeddings endpoint. Caller must set ai.openai.apiKey in application.properties
        String url = endpoint + "/embeddings";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey == null ? "" : apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("input", text);
        body.put("model", embeddingModel);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        double[] out = new double[0];
        try {
            ResponseEntity<Map> resp = rest.postForEntity(url, req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                // response shape: { data: [ { embedding: [...], index: 0 } ], model: "..." }
                Object dataObj = resp.getBody().get("data");
                if (dataObj instanceof List) {
                    List data = (List) dataObj;
                    if (!data.isEmpty() && data.get(0) instanceof Map) {
                        Object emb = ((Map) data.get(0)).get("embedding");
                        if (emb instanceof List) {
                            List nums = (List) emb;
                            out = new double[nums.size()];
                            for (int i = 0; i < nums.size(); i++) {
                                out[i] = ((Number) nums.get(i)).doubleValue();
                            }
                            return out;
                        }
                    }
                }
            }
        } catch (HttpClientErrorException ex) {
            // log and fallback
            System.err.println("OpenAI embedding request failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            System.err.println("OpenAI embedding error: " + ex.getMessage());
        }
        finally {
            sample.stop(Timer.builder("ai.openai.embedding.duration").description("OpenAI embedding call duration").register(meterRegistry));
        }

        return out;
    }

    public String chat(String prompt) {
        Timer.Sample sample = Timer.start(meterRegistry);
        // Simple chat placeholder using OpenAI chat completions if available.
        String url = endpoint + "/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey == null ? "" : apiKey);

        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> resp = rest.postForEntity(url, req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object choices = resp.getBody().get("choices");
                if (choices instanceof List && !((List) choices).isEmpty()) {
                    Object first = ((List) choices).get(0);
                    if (first instanceof Map) {
                        Object messageObj = ((Map) first).get("message");
                        if (messageObj instanceof Map) {
                            Object content = ((Map) messageObj).get("content");
                            return content == null ? "" : content.toString();
                        }
                    }
                }
            }
        } catch (HttpClientErrorException ex) {
            System.err.println("OpenAI chat request failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            System.err.println("OpenAI chat error: " + ex.getMessage());
        }
            finally {
                sample.stop(Timer.builder("ai.openai.chat.duration").description("OpenAI chat call duration").register(meterRegistry));
            }

            return "I'm a virtual librarian (fallback).";
    }
}
