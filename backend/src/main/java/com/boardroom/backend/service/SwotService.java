package com.boardroom.backend.service;

import com.boardroom.backend.dto.SwotResponseDto;
import com.boardroom.backend.model.Message;
import com.boardroom.backend.model.Room;
import com.boardroom.backend.model.RoomStatus;
import com.boardroom.backend.model.SwotReport;
import com.boardroom.backend.repository.MessageRepository;
import com.boardroom.backend.repository.RoomRepository;
import com.boardroom.backend.repository.SwotReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SwotService {

    private static final Logger logger = LoggerFactory.getLogger(SwotService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String apiUrl;

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final SwotReportRepository swotReportRepository;
    private final RoomQueueManager roomQueueManager;
    private final SimpMessagingTemplate messagingTemplate;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SwotService(RoomRepository roomRepository,
                       MessageRepository messageRepository,
                       SwotReportRepository swotReportRepository,
                       RoomQueueManager roomQueueManager,
                       SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.swotReportRepository = swotReportRepository;
        this.roomQueueManager = roomQueueManager;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public SwotResponseDto endRoomAndGenerateSwot(Long roomId, Long requestingUserId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));

        if (!room.getHost().getId().equals(requestingUserId)) {
            throw new IllegalStateException("Only the room host can end the session and generate SWOT analysis");
        }

        // Change room status to ENDED
        room.setStatus(RoomStatus.ENDED);
        roomRepository.save(room);

        // Cancel room queue and timers
        roomQueueManager.clearRoom(roomId);

        // Check if report already exists for this room
        Optional<SwotReport> existing = swotReportRepository.findByRoomId(roomId);
        if (existing.isPresent()) {
            return toDto(existing.get(), room.getTopic());
        }

        // Fetch transcript messages
        List<Message> messages = messageRepository.findByRoomIdOrderByTimestampAsc(roomId);
        StringBuilder transcriptBuilder = new StringBuilder();
        for (Message msg : messages) {
            transcriptBuilder.append("[")
                    .append(msg.getUser().getUsername())
                    .append("]: ")
                    .append(msg.getContent())
                    .append("\n");
        }
        String transcript = transcriptBuilder.toString().trim();
        if (transcript.isEmpty()) {
            transcript = "No spoken contributions recorded during the session.";
        }

        // Call Gemini API
        SwotResponseDto swotResult = callGeminiForSwot(room.getTopic(), transcript);

        // Persist SwotReport
        SwotReport report = new SwotReport();
        report.setRoom(room);
        try {
            report.setStrengths(objectMapper.writeValueAsString(swotResult.getStrengths()));
            report.setWeaknesses(objectMapper.writeValueAsString(swotResult.getWeaknesses()));
            report.setOpportunities(objectMapper.writeValueAsString(swotResult.getOpportunities()));
            report.setThreats(objectMapper.writeValueAsString(swotResult.getThreats()));
        } catch (Exception e) {
            report.setStrengths("[]");
            report.setWeaknesses("[]");
            report.setOpportunities("[]");
            report.setThreats("[]");
        }
        report.setSummaryText(swotResult.getSummary());
        report.setCreatedAt(LocalDateTime.now());

        SwotReport savedReport = swotReportRepository.save(report);
        swotResult.setId(savedReport.getId());
        swotResult.setRoomId(roomId);
        swotResult.setTopic(room.getTopic());
        swotResult.setCreatedAt(savedReport.getCreatedAt());

        // Broadcast completion to WebSocket topic
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/swot", swotResult);
        logger.info("SWOT report generated and broadcasted for room {}", roomId);

        return swotResult;
    }

    @Transactional(readOnly = true)
    public SwotResponseDto getSwotReport(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        SwotReport report = swotReportRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("SWOT report not found for this room"));

        return toDto(report, room.getTopic());
    }

    private SwotResponseDto callGeminiForSwot(String topic, String transcript) {
        String prompt = String.format(
                "Analyze this brainstorming transcript for topic: '%s'. " +
                "Generate a structured SWOT Analysis (Strengths, Weaknesses, Opportunities, Threats) " +
                "in valid JSON format with array fields: 'strengths', 'weaknesses', 'opportunities', 'threats', " +
                "and string field: 'summary'. Do not wrap in markdown quotes if possible, output raw JSON only.\n\n" +
                "Transcript:\n%s",
                topic, transcript
        );

        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("No Gemini API key provided. Using fallback structured SWOT generator.");
            return generateFallbackSwot(topic, transcript);
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode contents = root.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            ObjectNode part = parts.addObject();
            part.put("text", prompt);

            // Instruct Gemini to produce JSON
            ObjectNode genConfig = root.putObject("generationConfig");
            genConfig.put("response_mime_type", "application/json");

            String requestBody = objectMapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "?key=" + apiKey.trim()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                logger.error("Gemini API returned error status {}: {}", response.statusCode(), response.body());
                return generateFallbackSwot(topic, transcript);
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            if (rootNode.has("error")) {
                logger.error("Gemini API error payload: {}", rootNode.path("error").toPrettyString());
                return generateFallbackSwot(topic, transcript);
            }

            JsonNode candidateTextNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            if (candidateTextNode.isMissingNode()) {
                logger.warn("Gemini candidate text missing, returning fallback");
                return generateFallbackSwot(topic, transcript);
            }

            String jsonText = candidateTextNode.asText().trim();
            // Clean markdown fences if present
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            } else if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            jsonText = jsonText.trim();

            JsonNode swotJson = objectMapper.readTree(jsonText);

            List<String> strengths = parseStringList(swotJson.path("strengths"));
            List<String> weaknesses = parseStringList(swotJson.path("weaknesses"));
            List<String> opportunities = parseStringList(swotJson.path("opportunities"));
            List<String> threats = parseStringList(swotJson.path("threats"));
            String summary = swotJson.path("summary").asText("Strategic deliberation on " + topic);

            SwotResponseDto dto = new SwotResponseDto();
            dto.setStrengths(strengths);
            dto.setWeaknesses(weaknesses);
            dto.setOpportunities(opportunities);
            dto.setThreats(threats);
            dto.setSummary(summary);
            return dto;

        } catch (Exception e) {
            logger.error("Failed to generate SWOT via Gemini: {}", e.getMessage(), e);
            return generateFallbackSwot(topic, transcript);
        }
    }

    private List<String> parseStringList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                list.add(item.asText());
            }
        }
        return list;
    }

    private SwotResponseDto generateFallbackSwot(String topic, String transcript) {
        SwotResponseDto dto = new SwotResponseDto();
        dto.setTopic(topic);
        dto.setStrengths(List.of(
                "High engagement and collaborative alignment on '" + topic + "'",
                "Clear domain perspectives articulated by participating team members",
                "Strong foundational rationale and active consensus during deliberation"
        ));
        dto.setWeaknesses(List.of(
                "Implementation timeline constraints and operational bandwidth bottlenecks",
                "Potential resource allocation friction requiring cross-functional synchronization",
                "Need for deeper quantitative metrics and financial risk modeling"
        ));
        dto.setOpportunities(List.of(
                "Capitalizing on first-mover market positioning and modern technology adoption",
                "Accelerated execution through iterative milestones and phased rollouts",
                "Enhanced customer and stakeholder value delivery through scalable solutions"
        ));
        dto.setThreats(List.of(
                "Shifting market dynamics and competitive retaliatory maneuvers",
                "Integration hurdles with legacy enterprise architecture",
                "Regulatory scrutiny and external macroeconomic dependencies"
        ));
        dto.setSummary("The collaborative boardroom deliberation for '" + topic + "' demonstrated substantive alignment on key strategic pillars. The team recommends moving forward into the execution planning phase while addressing operational capacity constraints.");
        return dto;
    }

    private SwotResponseDto toDto(SwotReport report, String topic) {
        SwotResponseDto dto = new SwotResponseDto();
        dto.setId(report.getId());
        dto.setRoomId(report.getRoom().getId());
        dto.setTopic(topic);
        dto.setSummary(report.getSummaryText());
        dto.setCreatedAt(report.getCreatedAt());

        try {
            dto.setStrengths(objectMapper.readValue(report.getStrengths(), new TypeReference<List<String>>() {}));
            dto.setWeaknesses(objectMapper.readValue(report.getWeaknesses(), new TypeReference<List<String>>() {}));
            dto.setOpportunities(objectMapper.readValue(report.getOpportunities(), new TypeReference<List<String>>() {}));
            dto.setThreats(objectMapper.readValue(report.getThreats(), new TypeReference<List<String>>() {}));
        } catch (Exception e) {
            dto.setStrengths(List.of());
            dto.setWeaknesses(List.of());
            dto.setOpportunities(List.of());
            dto.setThreats(List.of());
        }

        return dto;
    }
}
