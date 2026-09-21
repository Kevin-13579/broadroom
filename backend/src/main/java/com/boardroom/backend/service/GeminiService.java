package com.boardroom.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String apiUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateRoleResponse(String roleTitle, String roleDirective, String topic, String recentTranscript) {
        try {
            String promptText = String.format(
                "You are participating in a boardroom brainstorming session.\n" +
                "Topic: %s\n" +
                "Your Assigned Role: %s\n" +
                "Persona Rules: %s\n\n" +
                "Recent Boardroom Speech History:\n%s\n\n" +
                "Task: Speak in 2-3 concise sentences strictly from your assigned role's perspective. Evaluate if the current idea is practical or identify field-specific blockers. Do not use quotes.",
                topic, roleTitle, roleDirective, recentTranscript
            );

            com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
            com.fasterxml.jackson.databind.node.ArrayNode contents = root.putArray("contents");
            com.fasterxml.jackson.databind.node.ObjectNode content = contents.addObject();
            com.fasterxml.jackson.databind.node.ArrayNode parts = content.putArray("parts");
            com.fasterxml.jackson.databind.node.ObjectNode part = parts.addObject();
            part.put("text", promptText);

            String requestBody = objectMapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode rootNode = objectMapper.readTree(response.body());
            
            // Check for error responses
            if (rootNode.has("error")) {
                System.err.println("Gemini API Error: " + rootNode.path("error").toPrettyString());
                return "[" + roleTitle + "]: API error encountered.";
            }

            JsonNode candidateTextNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            if (!candidateTextNode.isMissingNode()) {
                return candidateTextNode.asText().trim();
            }

            return "[" + roleTitle + "]: I see potential challenges here from my domain perspective.";

        } catch (Exception e) {
            return "[" + roleTitle + "]: Technical difficulty evaluating point.";
        }
    }

    public String generateSwotAnalysis(String topic, String fullTranscript) {
        try {
            String promptText = String.format(
                "You are an expert business analyst and meeting facilitator. Review the following boardroom brainstorming transcript for the topic: '%s'.\n\n" +
                "Generate a highly professional, well-structured markdown report including:\n" +
                "1. A concise Executive Summary.\n" +
                "2. A SWOT Analysis (Strengths, Weaknesses, Opportunities, Threats).\n" +
                "3. Key positive takeaways and key negative concerns raised by the roles.\n" +
                "4. Win metrics and failure/success rate probability based on the tone and content.\n\n" +
                "Transcript:\n%s",
                topic, fullTranscript
            );

            com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
            com.fasterxml.jackson.databind.node.ArrayNode contents = root.putArray("contents");
            com.fasterxml.jackson.databind.node.ObjectNode content = contents.addObject();
            com.fasterxml.jackson.databind.node.ArrayNode parts = content.putArray("parts");
            com.fasterxml.jackson.databind.node.ObjectNode part = parts.addObject();
            part.put("text", promptText);

            String requestBody = objectMapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode rootNode = objectMapper.readTree(response.body());
            
            if (rootNode.has("error")) {
                System.err.println("Gemini API Error: " + rootNode.path("error").toPrettyString());
                return "## Analysis Failed\nAn API error occurred while generating the analysis.";
            }

            JsonNode candidateTextNode = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text");

            if (!candidateTextNode.isMissingNode()) {
                return candidateTextNode.asText().trim();
            }

            return "## Analysis Unavailable\nCould not parse the AI response.";

        } catch (Exception e) {
            e.printStackTrace();
            return "## Technical Difficulty\nFailed to generate the analysis.";
        }
    }
}