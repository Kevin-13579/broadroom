package com.boardroom.backend.service;

import com.boardroom.backend.dto.RoleSkillset;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SkillsetLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillsetLoader.class);

    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourcePatternResolver;

    private final Map<String, RoleSkillset> skillsetByRole = new ConcurrentHashMap<>();
    private final Map<String, RoleSkillset> normalizedLookupMap = new ConcurrentHashMap<>();

    public SkillsetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
    }

    @PostConstruct
    public void init() {
        loadSkillsets();
    }

    public synchronized void loadSkillsets() {
        skillsetByRole.clear();
        normalizedLookupMap.clear();

        Set<String> processedFilenames = new HashSet<>();

        // 1. Attempt loading from classpath: skillset/*.json
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath*:skillset/*.json");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && filename.endsWith(".json")) {
                    try (InputStream is = resource.getInputStream()) {
                        RoleSkillset skillset = objectMapper.readValue(is, RoleSkillset.class);
                        registerSkillset(filename, skillset);
                        processedFilenames.add(filename);
                    } catch (Exception e) {
                        log.error("Failed to parse classpath skillset JSON: {}", filename, e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not scan classpath for skillset JSON files: {}", e.getMessage());
        }

        // 2. Fallback / supplement from filesystem directories: "skillset" and "backend/skillset"
        List<Path> candidateDirs = List.of(
                Paths.get("skillset"),
                Paths.get("backend", "skillset"),
                Paths.get("src", "main", "resources", "skillset")
        );

        for (Path dir : candidateDirs) {
            if (Files.isDirectory(dir)) {
                try {
                    File[] files = dir.toFile().listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
                    if (files != null) {
                        for (File file : files) {
                            if (!processedFilenames.contains(file.getName())) {
                                try (InputStream is = new FileInputStream(file)) {
                                    RoleSkillset skillset = objectMapper.readValue(is, RoleSkillset.class);
                                    registerSkillset(file.getName(), skillset);
                                    processedFilenames.add(file.getName());
                                } catch (Exception e) {
                                    log.error("Failed to parse filesystem skillset JSON: {}", file.getAbsolutePath(), e);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not read skillset directory {}: {}", dir, e.getMessage());
                }
            }
        }

        log.info("SkillsetLoader initialized with {} unique role skillsets cached.", skillsetByRole.size());
    }

    private void registerSkillset(String filename, RoleSkillset skillset) {
        if (skillset == null) {
            return;
        }

        String role = skillset.getRole();
        if (role != null && !role.isBlank()) {
            skillsetByRole.put(role, skillset);
            normalizedLookupMap.put(normalizeKey(role), skillset);
        }

        if (filename != null) {
            String baseName = filename.replaceFirst("\\.json$", "");
            normalizedLookupMap.put(normalizeKey(baseName), skillset);
        }
    }

    public static String normalizeKey(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    public Optional<RoleSkillset> findSkillset(String roleOrKey) {
        if (roleOrKey == null || roleOrKey.isBlank()) {
            return Optional.empty();
        }

        // Direct match
        RoleSkillset direct = skillsetByRole.get(roleOrKey);
        if (direct != null) {
            return Optional.of(direct);
        }

        // Normalized match
        RoleSkillset normalized = normalizedLookupMap.get(normalizeKey(roleOrKey));
        return Optional.ofNullable(normalized);
    }

    public RoleSkillset getSkillset(String roleOrKey) {
        return findSkillset(roleOrKey).orElse(null);
    }

    public Map<String, RoleSkillset> getAllSkillsets() {
        return Collections.unmodifiableMap(skillsetByRole);
    }

    public int getLoadedCount() {
        return skillsetByRole.size();
    }
}
