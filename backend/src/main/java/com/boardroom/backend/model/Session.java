package com.boardroom.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity 
@Table(name="sessions")
public class Session {

    @Id 
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String topic;

    @Column(columnDefinition = "TEXT")
    private String problemStatement;

    private String domain;
    private String theme;
    private String hostingPreference;
    private String budgetTier;

    private String sessionType = "CLIENT"; // "CLIENT" or "INNOVATIVE"

    @Column(columnDefinition = "TEXT")
    private String proposedSolution;

    private int numberOfCharacters;
    private int durationMinutes;
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean active = true;

    public Session() {
    }

    public Session(Long id, String topic, String problemStatement, String domain, String theme, 
                   String hostingPreference, String budgetTier, int numberOfCharacters, 
                   int durationMinutes, LocalDateTime createdAt, boolean active) {
        this.id = id;
        this.topic = topic;
        this.problemStatement = problemStatement;
        this.domain = domain;
        this.theme = theme;
        this.hostingPreference = hostingPreference;
        this.budgetTier = budgetTier;
        this.numberOfCharacters = numberOfCharacters;
        this.durationMinutes = durationMinutes;
        this.createdAt = createdAt;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getHostingPreference() { return hostingPreference; }
    public void setHostingPreference(String hostingPreference) { this.hostingPreference = hostingPreference; }

    public String getBudgetTier() { return budgetTier; }
    public void setBudgetTier(String budgetTier) { this.budgetTier = budgetTier; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public String getProposedSolution() { return proposedSolution; }
    public void setProposedSolution(String proposedSolution) { this.proposedSolution = proposedSolution; }

    public int getNumberOfCharacters() { return numberOfCharacters; }
    public void setNumberOfCharacters(int numberOfCharacters) { this.numberOfCharacters = numberOfCharacters; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
