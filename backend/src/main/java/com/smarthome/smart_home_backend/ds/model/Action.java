package com.smarthome.smart_home_backend.ds.model;

public class Action {
    private final int deviceId;
    private final String command;

    public Action(int deviceId, String command) {
        this.deviceId = deviceId;
        this.command = command;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "Action{" +
                "deviceId=" + deviceId +
                ", command='" + command + '\'' +
                '}';
    }
}