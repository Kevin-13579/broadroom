package com.boardroom.backend.dto;

import java.time.LocalDateTime;

public class RoomMemberDto {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private boolean isHost;
    private LocalDateTime joinedAt;

    public RoomMemberDto() {
    }

    public RoomMemberDto(Long id, Long userId, String username, String email, boolean isHost, LocalDateTime joinedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.isHost = isHost;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
