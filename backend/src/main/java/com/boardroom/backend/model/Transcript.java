package com.boardroom.backend.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity 
@Table(name="transcripts")
public class Transcript {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long sessionId;
    private String speakerName;
    private String speakerRole;
    @Column(columnDefinition="TEXT")
    private String messageText;
    private LocalDateTime timestamp=LocalDateTime.now();

    public Transcript() {
    }

    public Transcript(Long id, Long sessionId, String speakerName, String speakerRole, String messageText, LocalDateTime timestamp) {
        this.id = id;
        this.sessionId = sessionId;
        this.speakerName = speakerName;
        this.speakerRole = speakerRole;
        this.messageText = messageText;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getSpeakerName() { return speakerName; }
    public void setSpeakerName(String speakerName) { this.speakerName = speakerName; }
    public String getSpeakerRole() { return speakerRole; }
    public void setSpeakerRole(String speakerRole) { this.speakerRole = speakerRole; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
