package com.boardroom.backend.service;

import com.boardroom.backend.dto.RoleSkillset;
import com.boardroom.backend.model.Session;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class OllamaService {

    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);

    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String apiUrl;

    @Value("${ollama.model.name:llama3.2}")
    private String modelName;

    private final SkillsetLoader skillsetLoader;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaService(SkillsetLoader skillsetLoader, ObjectMapper objectMapper) {
        this.skillsetLoader = skillsetLoader;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String generateRoleResponse(String roleTitle, String topic, String recentTranscript) {
        return generateRoleResponse(roleTitle, null, topic, recentTranscript);
    }

    public String generateRoleResponse(String roleTitle, String fallbackDirective, String topic, String recentTranscript) {
        Session dummySession = new Session();
        dummySession.setTopic(topic);
        return generateRoleResponse(roleTitle, fallbackDirective, dummySession, recentTranscript);
    }

    public String generateRoleResponse(String roleTitle, String fallbackDirective, Session session, String recentTranscript) {
        try {
            String promptText = buildRolePrompt(roleTitle, fallbackDirective, session, recentTranscript);

            ObjectNode requestJson = objectMapper.createObjectNode();
            requestJson.put("model", modelName);
            requestJson.put("prompt", promptText);
            requestJson.put("stream", false);

            String requestBody = objectMapper.writeValueAsString(requestJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                if (rootNode.has("response")) {
                    return rootNode.get("response").asText().trim();
                }
                log.warn("Ollama response did not contain 'response' field: {}", response.body());
                return "[" + roleTitle + "]: I see potential challenges here from my domain perspective.";
            } else {
                log.error("Ollama HTTP Error {}: {}", response.statusCode(), response.body());
                return "[" + roleTitle + "]: API error encountered (HTTP " + response.statusCode() + ").";
            }

        } catch (Exception e) {
            log.error("Failed to generate Ollama role response for role '{}': {}", roleTitle, e.getMessage());
            return "[" + roleTitle + "]: Technical difficulty evaluating point.";
        }
    }

    public String buildRolePrompt(String roleTitle, String fallbackDirective, String topic, String recentTranscript) {
        Session session = new Session();
        session.setTopic(topic);
        return buildRolePrompt(roleTitle, fallbackDirective, session, recentTranscript);
    }

    public String buildRolePrompt(String roleTitle, String fallbackDirective, Session session, String recentTranscript) {
        StringBuilder systemSection = new StringBuilder();
        RoleSkillset skillset = skillsetLoader.findSkillset(roleTitle).orElse(null);

        if (skillset != null) {
            systemSection.append("You are a professional board member participating in a corporate client project kickoff meeting.\n");
            systemSection.append("Your Assigned Role: ").append(skillset.getRole() != null ? skillset.getRole() : roleTitle).append("\n");
            if (skillset.getFocus() != null && !skillset.getFocus().isBlank()) {
                systemSection.append("Focus Area: ").append(skillset.getFocus()).append("\n");
            }
            if (skillset.getSkills() != null && !skillset.getSkills().isEmpty()) {
                systemSection.append("Core Skills: ").append(String.join(", ", skillset.getSkills())).append("\n");
            }
            if (skillset.getResponsibilities() != null && !skillset.getResponsibilities().isEmpty()) {
                systemSection.append("Key Responsibilities:\n");
                for (String resp : skillset.getResponsibilities()) {
                    systemSection.append("  - ").append(resp).append("\n");
                }
            }
            if (skillset.getDos() != null && !skillset.getDos().isEmpty()) {
                systemSection.append("Behavioral Guidelines (DOs):\n");
                for (String d : skillset.getDos()) {
                    systemSection.append("  - ").append(d).append("\n");
                }
            }
            if (skillset.getDonts() != null && !skillset.getDonts().isEmpty()) {
                systemSection.append("Strict Constraints (DON'Ts):\n");
                for (String dont : skillset.getDonts()) {
                    systemSection.append("  - ").append(dont).append("\n");
                }
            }
        } else {
            systemSection.append("You are a professional board member participating in a corporate client project kickoff meeting.\n");
            systemSection.append("Your Assigned Role: ").append(roleTitle).append("\n");
            String directive = (fallbackDirective != null && !fallbackDirective.isBlank())
                    ? fallbackDirective
                    : "Evaluate from your specialized domain experience.";
            systemSection.append("Persona Rules: ").append(directive).append("\n");
        }

        // Build Structured Brief Section based on sessionType
        StringBuilder projectBriefSection = new StringBuilder();
        boolean isInnovative = session != null && "INNOVATIVE".equalsIgnoreCase(session.getSessionType());

        if (isInnovative) {
            projectBriefSection.append("INNOVATIVE BRAINSTORMING CONCEPT:\n");
            if (session.getTopic() != null && !session.getTopic().isBlank()) {
                projectBriefSection.append("- Core Idea / Concept: ").append(session.getTopic()).append("\n");
            }
            if (session.getProblemStatement() != null && !session.getProblemStatement().isBlank()) {
                projectBriefSection.append("- Target Problem: ").append(session.getProblemStatement()).append("\n");
            }
            if (session.getProposedSolution() != null && !session.getProposedSolution().isBlank()) {
                projectBriefSection.append("- Proposed Solution / Mechanism: ").append(session.getProposedSolution()).append("\n");
            }
            projectBriefSection.append("- Engineering Domain: ").append(session.getDomain() != null ? session.getDomain() : "Software").append("\n");
        } else {
            projectBriefSection.append("CLIENT PROJECT INTAKE BRIEF:\n");
            String topic = (session != null && session.getTopic() != null && !session.getTopic().isBlank())
                    ? session.getTopic() : "Enterprise Client Project";
            projectBriefSection.append("- Project Name: ").append(topic).append("\n");

            if (session != null && session.getProblemStatement() != null && !session.getProblemStatement().isBlank()) {
                projectBriefSection.append("- Problem Statement: ").append(session.getProblemStatement()).append("\n");
            }
            if (session != null && session.getDomain() != null && !session.getDomain().isBlank()) {
                projectBriefSection.append("- Industry Domain: ").append(session.getDomain()).append("\n");
            }
            if (session != null && session.getTheme() != null && !session.getTheme().isBlank()) {
                projectBriefSection.append("- Delivery Platform / Theme: ").append(session.getTheme()).append("\n");
            }
            if (session != null && session.getHostingPreference() != null && !session.getHostingPreference().isBlank()) {
                projectBriefSection.append("- Hosting & Cloud Preference: ").append(session.getHostingPreference()).append("\n");
            }
            if (session != null && session.getBudgetTier() != null && !session.getBudgetTier().isBlank()) {
                projectBriefSection.append("- Budget & Scope Tier: ").append(session.getBudgetTier()).append("\n");
            }
        }

        String history = (recentTranscript != null && !recentTranscript.isBlank())
                ? recentTranscript
                : "Meeting kickoff just started.";

        String taskPrompt = isInnovative
                ? "Task: Speak in 2-3 concise sentences strictly from your assigned role's perspective. " +
                  "Critique the originality, technical hurdles, domain feasibility (" + (session != null && session.getDomain() != null ? session.getDomain() : "Software") + "), market demand, or unit economics of this idea. " +
                  "Do not use quotes or repetitive introductory fluff."
                : "Task: Speak in 2-3 concise sentences strictly from your assigned role's perspective. " +
                  "Critique technical feasibility, database choices, cloud hosting implications, MVP scope, or domain compliance for this client brief. " +
                  "Do not use quotes or repetitive introductory fluff.";

        return String.format(
                "%s\n" +
                "%s\n" +
                "Recent Boardroom Speech History:\n%s\n\n" +
                "%s",
                systemSection.toString().trim(),
                projectBriefSection.toString().trim(),
                history,
                taskPrompt
        );
    }

    public String generateSwotAnalysis(String topic, String fullTranscript) {
        Session session = new Session();
        session.setTopic(topic);
        return generateSwotAnalysis(session, fullTranscript);
    }

    public String generateSwotAnalysis(Session session, String fullTranscript) {
        try {
            boolean isInnovative = session != null && "INNOVATIVE".equalsIgnoreCase(session.getSessionType());
            String promptText;

            if (isInnovative) {
                String idea = session != null && session.getTopic() != null ? session.getTopic() : "Innovative Idea";
                String problem = session != null && session.getProblemStatement() != null ? session.getProblemStatement() : "N/A";
                String solution = session != null && session.getProposedSolution() != null ? session.getProposedSolution() : "N/A";
                String domain = session != null && session.getDomain() != null ? session.getDomain() : "Software";

                promptText = String.format(
                    "You are an expert venture builder, technical architect, and innovation consultant. Review the following boardroom brainstorming transcript for the concept: '%s'.\n\n" +
                    "INNOVATION PARAMETERS:\n" +
                    "- Idea / Line: %s\n" +
                    "- Problem Statement: %s\n" +
                    "- Proposed Solution: %s\n" +
                    "- Domain: %s\n\n" +
                    "Generate a professional, well-structured Innovation Assessment & Engineering Spec formatted in clean Markdown:\n" +
                    "1. **Executive Concept Summary & Novelty Evaluation**: Synthesis of the idea, core value proposition, and unique angle.\n" +
                    "2. **Recommended Technology & Engineering Stack**: Concrete frameworks, programming languages, database/cloud tools, or hardware/sensors/microcontrollers (%s domain) recommended to build an MVP.\n" +
                    "3. **SWOT Analysis (Strengths, Weaknesses, Opportunities, Threats)**: Rigorous breakdown based on the boardroom panel's dialogue.\n" +
                    "4. **MVP Prototyping Roadmap & Feasibility**: Practical phase 1 execution milestones, potential failure points, and success metrics.\n\n" +
                    "Transcript:\n%s",
                    idea, idea, problem, solution, domain, domain, fullTranscript
                );
            } else {
                String topic = session != null && session.getTopic() != null ? session.getTopic() : "Client Project";
                String problem = session != null && session.getProblemStatement() != null ? session.getProblemStatement() : "N/A";
                String domain = session != null && session.getDomain() != null ? session.getDomain() : "General Software";
                String theme = session != null && session.getTheme() != null ? session.getTheme() : "Web Application";
                String hosting = session != null && session.getHostingPreference() != null ? session.getHostingPreference() : "Cloud (AWS/GCP)";
                String budget = session != null && session.getBudgetTier() != null ? session.getBudgetTier() : "Lean MVP";

                promptText = String.format(
                    "You are an executive enterprise solutions architect and management consultant. Review the following corporate boardroom kickoff transcript for the project: '%s'.\n\n" +
                    "CLIENT SPECIFICATIONS:\n" +
                    "- Problem Statement: %s\n" +
                    "- Industry Domain: %s\n" +
                    "- Platform Theme: %s\n" +
                    "- Hosting & Cloud Target: %s\n" +
                    "- Budget Tier: %s\n\n" +
                    "Generate an executive-grade Project Kickoff Charter & Technical Architecture Proposal formatted in clean Markdown:\n" +
                    "1. **Executive Summary & Problem Validation**: Concise synthesis of client problem and alignment.\n" +
                    "2. **Technical Architecture & Infrastructure Strategy**: Recommended frontend, backend, database, and cloud hosting architecture on %s.\n" +
                    "3. **Core MVP Feature Breakdown**: Prioritized P0 essential workflow vs P1 features agreed upon.\n" +
                    "4. **Compliance, Security & Data Privacy Matrix**: Regulatory risks specific to the %s domain and safeguards.\n" +
                    "5. **SWOT & Risk Register**: Strengths, Weaknesses, Opportunities, and Threats with projected monthly hosting OPEX and delivery feasibility.\n\n" +
                    "Transcript:\n%s",
                    topic, problem, domain, theme, hosting, budget, hosting, domain, fullTranscript
                );
            }

            ObjectNode requestJson = objectMapper.createObjectNode();
            requestJson.put("model", modelName);
            requestJson.put("prompt", promptText);
            requestJson.put("stream", false);

            String requestBody = objectMapper.writeValueAsString(requestJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                if (rootNode.has("response")) {
                    return rootNode.get("response").asText().trim();
                }
                return "## Analysis Unavailable\nCould not parse Ollama response.";
            } else {
                return "## Analysis Failed\nOllama returned HTTP " + response.statusCode() + ".";
            }

        } catch (Exception e) {
            log.error("Failed to generate Ollama Project Charter / SWOT analysis: {}", e.getMessage());
            return "## Technical Difficulty\nFailed to generate the Project Charter using Ollama.";
        }
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getModelName() {
        return modelName;
    }
}
