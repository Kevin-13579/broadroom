package com.boardroom.backend.dto;

public class QueueUserDto {
    private Long userId;
    private String username;

    public QueueUserDto() {
    }

    public QueueUserDto(Long userId, String username) {
        this.userId = userId;
        this.username = username;
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
}
