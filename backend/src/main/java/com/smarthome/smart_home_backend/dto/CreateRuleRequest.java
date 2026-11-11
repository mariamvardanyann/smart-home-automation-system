package com.smarthome.smart_home_backend.dto;

import java.util.List;

public class CreateRuleRequest {
    public String name;
    public int homeId;

    public List<ConditionRequest> conditions;
    public List<ActionRequest> actions;

    public static class ConditionRequest {
        public int sensorId;
        public String operator;
        public double value;
    }

    public static class ActionRequest {
        public int deviceId;
        public String command;
    }
}