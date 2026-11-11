package com.smarthome.smart_home_backend.ds.model;

public class ExecutionRecord {
    private final int ruleId;
    private final String ruleName;
    private final int sensorId;
    private final double sensorValue;
    private final String actionCommand;
    private final int deviceId;
    private final long timestamp;
    private final String status;
    private final int eventId;
    private final String errorMessage;

    public ExecutionRecord(int ruleId,
                           String ruleName,
                           int sensorId,
                           double sensorValue,
                           String actionCommand,
                           int deviceId,
                           long timestamp,
                           String status) {
        this(ruleId, ruleName, sensorId, sensorValue, actionCommand, deviceId, timestamp, status, -1, null);
    }

    public ExecutionRecord(int ruleId, String ruleName, int sensorId, double sensorValue,
                           String actionCommand, int deviceId, long timestamp, String status,
                           int eventId, String errorMessage) {
        this.eventId = eventId;
        this.errorMessage = errorMessage;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.sensorId = sensorId;
        this.sensorValue = sensorValue;
        this.actionCommand = actionCommand;
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.status = status;
    }

    public int getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public int getSensorId() {
        return sensorId;
    }

    public double getSensorValue() {
        return sensorValue;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getEventId() { return eventId; }

    public String getErrorMessage() { return errorMessage; }

    public ExecutionRecord withStatus(String newStatus, String error) {
        return new ExecutionRecord(ruleId, ruleName, sensorId, sensorValue, actionCommand,
                deviceId, timestamp, newStatus, eventId, error);
    }

    public String getStatus() {
        return status;
    }



    @Override
    public String toString() {
        return "ExecutionRecord{" +
                "ruleId=" + ruleId +
                ", ruleName='" + ruleName + '\'' +
                ", sensorId=" + sensorId +
                ", sensorValue=" + sensorValue +
                ", actionCommand='" + actionCommand + '\'' +
                ", deviceId=" + deviceId +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }
}