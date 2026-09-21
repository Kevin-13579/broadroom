package com.boardroom.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SwotResponseDto {
    private Long id;
    private Long roomId;
    private String topic;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> opportunities;
    private List<String> threats;
    private String summary;
    private LocalDateTime createdAt;

    public SwotResponseDto() {
    }

    public SwotResponseDto(Long id, Long roomId, String topic, List<String> strengths, List<String> weaknesses, List<String> opportunities, List<String> threats, String summary, LocalDateTime createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.topic = topic;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.opportunities = opportunities;
        this.threats = threats;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(List<String> opportunities) {
        this.opportunities = opportunities;
    }

    public List<String> getThreats() {
        return threats;
    }

    public void setThreats(List<String> threats) {
        this.threats = threats;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
