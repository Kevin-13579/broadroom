package com.boardroom.backend.dto;

import com.boardroom.backend.model.RoomStatus;
import java.time.LocalDateTime;

public class RoomDto {
    private Long id;
    private String roomCode;
    private String topic;
    private UserDto host;
    private int maxMembers;
    private RoomStatus status;
    private int memberCount;
    private LocalDateTime createdAt;
    private boolean isHost;

    public RoomDto() {
    }

    public RoomDto(Long id, String roomCode, String topic, UserDto host, int maxMembers, RoomStatus status, int memberCount, LocalDateTime createdAt, boolean isHost) {
        this.id = id;
        this.roomCode = roomCode;
        this.topic = topic;
        this.host = host;
        this.maxMembers = maxMembers;
        this.status = status;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.isHost = isHost;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public UserDto getHost() {
        return host;
    }

    public void setHost(UserDto host) {
        this.host = host;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }
}
