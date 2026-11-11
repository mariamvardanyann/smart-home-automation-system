package com.smarthome.smart_home_backend.ds.structures;

import com.smarthome.smart_home_backend.ds.model.*;
import java.util.ArrayList;
import java.util.List;

public class SensorTrie {
    private final TrieNode root;

    public SensorTrie() {
        this.root = new TrieNode();
    }

    public void insertRule(Rule rule) {
        for (Condition condition : rule.getConditions()) {
            insertSensorId(condition.getSensorId(), rule);
        }
    }

    private void insertSensorId(int sensorId, Rule rule) {
        String sensorKey = String.valueOf(sensorId);
        TrieNode current = root;

        for (char ch : sensorKey.toCharArray()) {
            int digit = ch - '0';

            if (current.getChildren()[digit] == null) {
                current.getChildren()[digit] = new TrieNode();
            }

            current = current.getChildren()[digit];
        }

        current.setEndOfSensorId(true);

        if (!current.getRulesAtNode().contains(rule)) {
            current.getRulesAtNode().add(rule);
        }
    }

    public List<Rule> getRulesBySensorId(int sensorId) {
        String sensorKey = String.valueOf(sensorId);
        TrieNode current = root;

        for (char ch : sensorKey.toCharArray()) {
            int digit = ch - '0';

            if (current.getChildren()[digit] == null) {
                return new ArrayList<>();
            }

            current = current.getChildren()[digit];
        }

        if (current.isEndOfSensorId()) {
            return new ArrayList<>(current.getRulesAtNode());
        }

        return new ArrayList<>();
    }

    public boolean containsSensorId(int sensorId) {
        String sensorKey = String.valueOf(sensorId);
        TrieNode current = root;

        for (char ch : sensorKey.toCharArray()) {
            int digit = ch - '0';

            if (current.getChildren()[digit] == null) {
                return false;
            }

            current = current.getChildren()[digit];
        }

        return current.isEndOfSensorId();
    }

    public void printTrieRulesForSensor(int sensorId) {
        List<Rule> rules = getRulesBySensorId(sensorId);

        if (rules.isEmpty()) {
            System.out.println("No rules found for sensor ID: " + sensorId);
            return;
        }

        System.out.println("Rules for sensor ID " + sensorId + ":");
        for (Rule rule : rules) {
            System.out.println(rule);
        }
    }
}