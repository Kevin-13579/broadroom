package com.boardroom.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateRoomRequest {

    @NotBlank(message = "Topic is required")
    private String topic;

    @NotBlank(message = "Password is required")
    private String password;

    @Min(value = 3, message = "Maximum members must be at least 3")
    @Max(value = 10, message = "Maximum members cannot exceed 10")
    private int maxMembers = 6;

    public CreateRoomRequest() {
    }

    public CreateRoomRequest(String topic, String password, int maxMembers) {
        this.topic = topic;
        this.password = password;
        this.maxMembers = maxMembers;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
}
