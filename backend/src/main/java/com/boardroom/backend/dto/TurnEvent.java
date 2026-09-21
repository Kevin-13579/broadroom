package com.boardroom.backend.dto;

import java.util.List;

public class TurnEvent {
    private Long roomId;
    private Long activeUserId;
    private String username;
    private Long expiresAt;
    private List<QueueUserDto> queue;
    private String eventType = "TURN_CHANGE"; // "TURN_CHANGE", "TIME_UP", "TURN_PASSED"
    private String roomStatus; // "LOBBY", "ACTIVE", "ENDED"

    public TurnEvent() {
    }

    public TurnEvent(Long roomId, Long activeUserId, String username, Long expiresAt, List<QueueUserDto> queue) {
        this.roomId = roomId;
        this.activeUserId = activeUserId;
        this.username = username;
        this.expiresAt = expiresAt;
        this.queue = queue;
    }

    public TurnEvent(Long roomId, Long activeUserId, String username, Long expiresAt, List<QueueUserDto> queue, String eventType) {
        this.roomId = roomId;
        this.activeUserId = activeUserId;
        this.username = username;
        this.expiresAt = expiresAt;
        this.queue = queue;
        this.eventType = eventType;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getActiveUserId() {
        return activeUserId;
    }

    public void setActiveUserId(Long activeUserId) {
        this.activeUserId = activeUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<QueueUserDto> getQueue() {
        return queue;
    }

    public void setQueue(List<QueueUserDto> queue) {
        this.queue = queue;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(String roomStatus) {
        this.roomStatus = roomStatus;
    }
}
