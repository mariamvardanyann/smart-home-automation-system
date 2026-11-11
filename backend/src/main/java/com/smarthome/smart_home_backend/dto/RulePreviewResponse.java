package com.smarthome.smart_home_backend.dto;

public final class RulePreviewResponse {
    private final String ruleName;
    private final String result;
    public RulePreviewResponse(String ruleName, String result) {
        this.ruleName = ruleName; this.result = result;
    }
    public String getRuleName() { return ruleName; }
    public String getResult() { return result; }
}
