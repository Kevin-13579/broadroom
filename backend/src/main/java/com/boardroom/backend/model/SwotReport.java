package com.boardroom.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "swot_reports")
public class SwotReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(columnDefinition = "LONGTEXT")
    private String strengths;

    @Column(columnDefinition = "LONGTEXT")
    private String weaknesses;

    @Column(columnDefinition = "LONGTEXT")
    private String opportunities;

    @Column(columnDefinition = "LONGTEXT")
    private String threats;

    @Column(columnDefinition = "LONGTEXT")
    private String summaryText;

    private LocalDateTime createdAt = LocalDateTime.now();

    public SwotReport() {
    }

    public SwotReport(Long id, Room room, String strengths, String weaknesses, String opportunities, String threats, String summaryText, LocalDateTime createdAt) {
        this.id = id;
        this.room = room;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.opportunities = opportunities;
        this.threats = threats;
        this.summaryText = summaryText;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(String opportunities) {
        this.opportunities = opportunities;
    }

    public String getThreats() {
        return threats;
    }

    public void setThreats(String threats) {
        this.threats = threats;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
