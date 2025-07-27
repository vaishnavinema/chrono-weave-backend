package com.example.captionsense.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> generateCaption(String base64ImageData, String mimeType) {
        String prompt = "Generate a short, creative Instagram-style caption based on this image. Keep it emotional, engaging, and under 15 words. Include a relevant emoji.";

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> inlineData = Map.of("mimeType", mimeType, "data", base64ImageData);
        Map<String, Object> imagePart = Map.of("inlineData", inlineData);
        Map<String, Object> content = Map.of("role", "user", "parts", List.of(textPart, imagePart));
        Map<String, Object> payload = Map.of("contents", List.of(content));

        return webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractCaptionFromResult)
                .onErrorResume(e -> {
                    System.err.println("Error calling Gemini API: " + e.getMessage());
                    return Mono.just("Sorry, I couldn't think of a caption right now.");
                });
    }

    @SuppressWarnings("unchecked")
    private String extractCaptionFromResult(Map<String, Object> result) {

        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> firstCandidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return parts.get(0).get("text").replace("\"", "");
                }


            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini API response: " + e.getMessage());
        }
        return null;
    }
}
