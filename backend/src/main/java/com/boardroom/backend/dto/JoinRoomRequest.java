package com.boardroom.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class JoinRoomRequest {

    @NotBlank(message = "Room code is required")
    private String roomCode;

    @NotBlank(message = "Password is required")
    private String password;

    public JoinRoomRequest() {
    }

    public JoinRoomRequest(String roomCode, String password) {
        this.roomCode = roomCode;
        this.password = password;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
