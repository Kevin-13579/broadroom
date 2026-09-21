package com.boardroom.backend.service;

import com.boardroom.backend.model.Session;
import com.boardroom.backend.model.Transcript;
import com.boardroom.backend.repository.SessionRepository;
import com.boardroom.backend.repository.TranscriptRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BrainstormService {

    private final SessionRepository sessionRepository;
    private final TranscriptRepository transcriptRepository;
    private final OllamaService ollamaService;
    private final GeminiService geminiService;

    public BrainstormService(SessionRepository sessionRepository, 
                             TranscriptRepository transcriptRepository, 
                             OllamaService ollamaService, 
                             GeminiService geminiService) {
        this.sessionRepository = sessionRepository;
        this.transcriptRepository = transcriptRepository;
        this.ollamaService = ollamaService;
        this.geminiService = geminiService;
    }

    public static final Map<String, String> PRESET_ROLES = Map.ofEntries(
        Map.entry("Lead Developer", "Focus strictly on technical feasibility, code complexity, and architecture."),
        Map.entry("Product Manager", "Focus on core user workflow, MVP feature priorities, and user retention."),
        Map.entry("Venture Capitalist", "Focus on ROI, total addressable market, unit economics, and profit margin."),
        Map.entry("Security Specialist", "Focus on data leak vectors, user authentication vulnerabilities, and regulatory compliance."),
        Map.entry("UI/UX Designer", "Focus on interface layout, cognitive load, accessibility, and visual appeal."),
        Map.entry("Marketing Lead", "Focus on acquisition channels, brand strategy, and user growth hooks."),
        Map.entry("QA Lead Engineer", "Focus on edge-case bug scenarios, automated test coverage, and failure points."),
        Map.entry("CFO / Financial Controller", "Focus on server hosting operational costs, software licensing, and budget caps."),
        Map.entry("Legal Counsel", "Focus on IP rights, terms of service, liabilities, and copyright ownership."),
        Map.entry("DevOps Infrastructure Architect", "Focus on deployment pipelines, server load scalability, and uptime maintenance."),
        Map.entry("Customer Support Lead", "Focus on common user pain points, onboarding friction, and troubleshooting burden."),
        Map.entry("Data Privacy Officer", "Focus on GDPR, local data protection laws, and database encryption."),
        Map.entry("Operations Director", "Focus on team execution, realistic milestone pacing, and resource allocation."),
        Map.entry("Growth Hacker", "Focus on viral loops, referral schemes, and user retention metrics."),
        Map.entry("SEO / Content Manager", "Focus on search visibility, shareability, and target keywords."),
        Map.entry("Compliance Auditor", "Focus on industry standards, audit logs, and operational risk mitigation."),
        Map.entry("Scrum Master", "Focus on team velocity, delivery bottlenecks, and realistic sprint scoping."),
        Map.entry("HR / Talent Specialist", "Focus on team skill gaps, hiring needs, and workload balance."),
        Map.entry("Sales Lead", "Focus on conversion rates, sales pitches, and enterprise feature demands."),
        Map.entry("Chief Technology Officer", "Focus on overall technology strategy, scalability choices, and tech debt risks.")
    );

    public Session startSession(String topic, int numberOfCharacters, int durationMinutes) {
        return startSession(topic, null, null, null, null, null, "CLIENT", null, numberOfCharacters, durationMinutes);
    }

    public Session startSession(String topic, String problemStatement, String domain, String theme, 
                                String hostingPreference, String budgetTier, int numberOfCharacters, int durationMinutes) {
        return startSession(topic, problemStatement, domain, theme, hostingPreference, budgetTier, "CLIENT", null, numberOfCharacters, durationMinutes);
    }

    public Session startSession(String topic, String problemStatement, String domain, String theme, 
                                String hostingPreference, String budgetTier, String sessionType, 
                                String proposedSolution, int numberOfCharacters, int durationMinutes) {
        Session session = new Session();
        session.setTopic(topic);
        session.setProblemStatement(problemStatement);
        session.setDomain(domain);
        session.setTheme(theme);
        session.setHostingPreference(hostingPreference);
        session.setBudgetTier(budgetTier);
        session.setSessionType(sessionType != null ? sessionType : "CLIENT");
        session.setProposedSolution(proposedSolution);
        session.setNumberOfCharacters(numberOfCharacters);
        session.setDurationMinutes(durationMinutes);
        return sessionRepository.save(session);
    }

    public Transcript executeNpcTurn(Long sessionId, String speakerName, String speakerRole) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        List<Transcript> recent = transcriptRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        String context = recent.stream()
                .map(t -> t.getSpeakerName() + " (" + t.getSpeakerRole() + "): " + t.getMessageText())
                .collect(Collectors.joining("\n"));

        String directive = PRESET_ROLES.getOrDefault(speakerRole, "Evaluate from your specialized domain experience.");
        String responseText = ollamaService.generateRoleResponse(speakerRole, directive, session, context);

        Transcript transcript = new Transcript();
        transcript.setSessionId(sessionId);
        transcript.setSpeakerName(speakerName);
        transcript.setSpeakerRole(speakerRole);
        transcript.setMessageText(responseText);

        return transcriptRepository.save(transcript);
    }

    public Transcript recordUserPoint(Long sessionId, String userMessage) {
        Transcript transcript = new Transcript();
        transcript.setSessionId(sessionId);
        transcript.setSpeakerName("Host (You)");
        transcript.setSpeakerRole("Session Facilitator");
        transcript.setMessageText(userMessage);
        return transcriptRepository.save(transcript);
    }

    public List<Transcript> getTranscript(Long sessionId) {
        return transcriptRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    public Map<String, String> getSwotAnalysis(Long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        List<Transcript> transcriptList = transcriptRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        String fullTranscript = transcriptList.stream()
                .map(t -> t.getSpeakerName() + " (" + t.getSpeakerRole() + "): " + t.getMessageText())
                .collect(Collectors.joining("\n"));

        String analysis = ollamaService.generateSwotAnalysis(session, fullTranscript);

        return Map.of("analysis", analysis);
    }
}