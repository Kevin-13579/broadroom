package com.boardroom.backend.controller;

import com.boardroom.backend.model.Session;
import com.boardroom.backend.model.Transcript;
import com.boardroom.backend.service.BrainstormService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final BrainstormService brainstormService;

    public SessionController(BrainstormService brainstormService) {
        this.brainstormService = brainstormService;
    }

    @GetMapping("/roles")
    public Set<String> getPresetRoles() {
        return BrainstormService.PRESET_ROLES.keySet();
    }

    @PostMapping("/start")
    public Session startSession(@RequestBody Map<String, Object> payload) {
        String topic = (String) payload.get("topic");
        String problemStatement = (String) payload.get("problemStatement");
        String domain = (String) payload.get("domain");
        String theme = (String) payload.get("theme");
        String hostingPreference = (String) payload.get("hostingPreference");
        String budgetTier = (String) payload.get("budgetTier");
        String sessionType = (String) payload.getOrDefault("sessionType", "CLIENT");
        String proposedSolution = (String) payload.get("proposedSolution");
        int numberOfCharacters = payload.get("numberOfCharacters") != null 
                ? Integer.parseInt(payload.get("numberOfCharacters").toString()) : 6;
        int durationMinutes = payload.get("durationMinutes") != null 
                ? Integer.parseInt(payload.get("durationMinutes").toString()) : 15;
        return brainstormService.startSession(topic, problemStatement, domain, theme, 
                hostingPreference, budgetTier, sessionType, proposedSolution, numberOfCharacters, durationMinutes);
    }

    @PostMapping("/{id}/npc-turn")
    public Transcript triggerNpcTurn(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String speakerName = payload.get("speakerName");
        String speakerRole = payload.get("speakerRole");
        return brainstormService.executeNpcTurn(id, speakerName, speakerRole);
    }

    @PostMapping("/{id}/user-bid")
    public Transcript postUserPoint(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        return brainstormService.recordUserPoint(id, message);
    }

    @GetMapping("/{id}/transcript")
    public List<Transcript> getTranscript(@PathVariable Long id) {
        return brainstormService.getTranscript(id);
    }

    @GetMapping("/{id}/analysis")
    public Map<String, String> getSwotAnalysis(@PathVariable Long id) {
        return brainstormService.getSwotAnalysis(id);
    }
}