package com.email.writer.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {

        // Build prompt
        String prompt = buildPrompt(emailRequest);

        // Craft request body
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        // Send request to Gemini API
        String response = webClient.post()
                .uri(geminiApiUrl + "?key=" + geminiApiKey)  // FIXED
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)                     // FIXED (was missing)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractResponseContent(response);
    }

    public String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            return rootNode
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

    public String buildPrompt(EmailRequest emailRequest) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content. ")
              .append("Please don't generate a subject line. ");

        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ")
                  .append(emailRequest.getTone())
                  .append(" tone. ");
        }

        prompt.append("\n\nOriginal email:\n")
              .append(emailRequest.getEmailContent());

        return prompt.toString();
    }
}