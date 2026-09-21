package com.boardroom.backend.dto;

import java.time.LocalDateTime;

public class MessageDto {
    private Long id;
    private Long roomId;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime timestamp;

    public MessageDto() {
    }

    public MessageDto(Long id, Long roomId, Long userId, String username, String content, LocalDateTime timestamp) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.timestamp = timestamp;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
