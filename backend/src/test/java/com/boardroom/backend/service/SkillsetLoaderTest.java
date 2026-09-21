package com.boardroom.backend.service;

import com.boardroom.backend.dto.RoleSkillset;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SkillsetLoaderTest {

    private SkillsetLoader skillsetLoader;
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        skillsetLoader = new SkillsetLoader(objectMapper);
        skillsetLoader.loadSkillsets();
        ollamaService = new OllamaService(skillsetLoader, objectMapper);
    }

    @Test
    void testAllSkillsetsLoadedAndValid() {
        Map<String, RoleSkillset> skillsets = skillsetLoader.getAllSkillsets();
        assertFalse(skillsets.isEmpty(), "Skillsets map should not be empty");
        assertTrue(skillsets.size() >= 20, "Should load at least 20 skillsets, actual: " + skillsets.size());

        for (Map.Entry<String, RoleSkillset> entry : skillsets.entrySet()) {
            RoleSkillset s = entry.getValue();
            assertNotNull(s.getRole(), "Role must not be null in " + entry.getKey());
            assertFalse(s.getRole().isBlank(), "Role must not be blank in " + entry.getKey());
            assertNotNull(s.getFocus(), "Focus must not be null in " + entry.getKey());
            assertFalse(s.getFocus().isBlank(), "Focus must not be blank in " + entry.getKey());
            assertNotNull(s.getSkills(), "Skills list must not be null in " + entry.getKey());
            assertFalse(s.getSkills().isEmpty(), "Skills list must not be empty in " + entry.getKey());
            assertNotNull(s.getResponsibilities(), "Responsibilities must not be null in " + entry.getKey());
            assertFalse(s.getResponsibilities().isEmpty(), "Responsibilities must not be empty in " + entry.getKey());
            assertNotNull(s.getDos(), "DOs must not be null in " + entry.getKey());
            assertFalse(s.getDos().isEmpty(), "DOs must not be empty in " + entry.getKey());
            assertNotNull(s.getDonts(), "DON'Ts must not be null in " + entry.getKey());
            assertFalse(s.getDonts().isEmpty(), "DON'Ts must not be empty in " + entry.getKey());
        }
    }

    @Test
    void testLookupMethods() {
        // Direct role lookup
        RoleSkillset leadDev = skillsetLoader.getSkillset("Lead Developer");
        assertNotNull(leadDev, "Should find 'Lead Developer'");
        assertEquals("Lead Developer", leadDev.getRole());

        // Normalized key lookup
        RoleSkillset cfo = skillsetLoader.getSkillset("cfo_financial_controller");
        assertNotNull(cfo, "Should find CFO by file base name");
        assertEquals("CFO / Financial Controller", cfo.getRole());

        RoleSkillset devOps = skillsetLoader.getSkillset("DevOps Infrastructure Architect");
        assertNotNull(devOps, "Should find DevOps Infrastructure Architect");
    }

    @Test
    void testPromptBuildingWithSkillset() {
        String prompt = ollamaService.buildRolePrompt(
                "Lead Developer",
                null,
                "Microservices vs Monolith",
                "Product Manager (Product Manager): We need to launch fast."
        );

        assertTrue(prompt.contains("Lead Developer"));
        assertTrue(prompt.contains("Focus Area:"));
        assertTrue(prompt.contains("Technical feasibility"));
        assertTrue(prompt.contains("Behavioral Guidelines (DOs):"));
        assertTrue(prompt.contains("Strict Constraints (DON'Ts):"));
        assertTrue(prompt.contains("Microservices vs Monolith"));
    }

    @Test
    void testPromptBuildingWithGracefulFallback() {
        String prompt = ollamaService.buildRolePrompt(
                "Astronaut",
                "Evaluate space flight risks.",
                "Mars Mission",
                ""
        );

        assertTrue(prompt.contains("Your Assigned Role: Astronaut"));
        assertTrue(prompt.contains("Evaluate space flight risks."));
    }
}
