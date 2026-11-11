package com.smarthome.smart_home_backend.dto;

public class ReadableLogResponse {
    private String time;
    private String action;
    private String reason;
    private String ruleName;
    private String status;

    public ReadableLogResponse(String time, String action, String reason, String ruleName, String status) {
        this.time = time;
        this.action = action;
        this.reason = reason;
        this.ruleName = ruleName;
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getStatus() {
        return status;
    }
}