package com.smarthome.smart_home_backend.facade;

import com.smarthome.smart_home_backend.db.DatabaseLoader;
import com.smarthome.smart_home_backend.ds.engine.RuleEngine;
import com.smarthome.smart_home_backend.ds.model.*;
import com.smarthome.smart_home_backend.dto.CreateRuleRequest;
import com.smarthome.smart_home_backend.dto.RulePreviewResponse;
import com.smarthome.smart_home_backend.model.Device;
import org.springframework.stereotype.Component;

import java.util.*;

/** Coordinates database persistence with the pure rule engine. All state access is serialized. */
@Component
public class AutomationFacade {
    private final RuleEngine ruleEngine = new RuleEngine();
    private final Map<Integer, Device> devices = new LinkedHashMap<>();
    private final List<ExecutionRecord> appliedLog = new ArrayList<>();
    private int appliedLogCount = 0;

    public AutomationFacade() {
        // Fail at startup if the database is unavailable; do not silently start an empty system.
        for (Device device : DatabaseLoader.loadDevices()) devices.put(device.getId(), device);
        ruleEngine.addRules(DatabaseLoader.loadRules());
    }

    public synchronized List<Device> getAllDevices() { return List.copyOf(devices.values()); }

    public synchronized Device toggleDevice(int id) {
        Device old = devices.get(id);
        if (old == null) throw new NoSuchElementException("Device not found: " + id);
        if (!old.isActive()) throw new IllegalArgumentException("Device is inactive: " + id);
        boolean newState = !old.isOn();
        DatabaseLoader.updateDeviceState(id, newState); // DB first
        Device updated = copyWithState(old, newState);
        devices.put(id, updated);
        return updated;
    }

    private Device copyWithState(Device old, boolean state) {
        return new Device(old.getId(), old.getName(), old.getRoom(), old.getType(), state, old.isActive());
    }

    public synchronized String getRulesDebug() { return ruleEngine.getRules().toString(); }
    public synchronized List<Rule> getAllRules() { return List.copyOf(ruleEngine.getRules().values()); }
    public synchronized List<ExecutionRecord> getExecutionLog() { return List.copyOf(appliedLog); }

    public synchronized void triggerEvent(int sensorId, double value) {
        if (sensorId <= 0 || !Double.isFinite(value))
            throw new IllegalArgumentException("Invalid sensor ID or sensor value");
        // Each reading is stored just once; its database ID travels with the queued event.
        int eventId = DatabaseLoader.saveEvent(sensorId, value);
        ruleEngine.triggerEvent(sensorId, value, eventId);
    }

    public synchronized void processAllEvents() {
        ruleEngine.processAllEvents();
        List<ExecutionRecord> planned = ruleEngine.getExecutionLog();
        for (int i = appliedLogCount; i < planned.size(); i++) {
            applyAndLog(planned.get(i));
            appliedLogCount = i + 1;
        }
    }

    private void applyAndLog(ExecutionRecord record) {
        Device device = devices.get(record.getDeviceId());
        if (device == null || !device.isActive()) {
            recordFailure(record, "Target device unavailable");
            return;
        }
        String command = record.getActionCommand();
        String normalized = command == null ? "" : command.toLowerCase(Locale.ROOT).trim();
        if (normalized.contains(" [")) normalized = normalized.substring(0, normalized.indexOf(" ["));
        Boolean newState;
        String outcome = "SIMULATED"; // This prototype does not control real hardware.
        if (normalized.equals("on") || normalized.equals("turn_on") || normalized.equals("turn on")
                || normalized.startsWith("turn_on_")) {
            newState = true;
        } else if (normalized.equals("off") || normalized.equals("turn_off") || normalized.equals("turn off")
                || normalized.startsWith("turn_off_") || normalized.startsWith("shut_off")) {
            newState = false;
        } else if (normalized.equals("send_alert")) {
            newState = null; // Alert action is logged; no real notification is delivered.
        } else {
            recordFailure(record, "Unsupported action command");
            return;
        }
        ExecutionRecord completed = record.withStatus(outcome, null);
        try {
            DatabaseLoader.applyDeviceActionAndLog(completed, newState);
            if (newState != null) devices.put(device.getId(), copyWithState(device, newState));
            appliedLog.add(completed);
        } catch (RuntimeException ex) {
            recordFailure(record, "Database action failed");
        }
    }

    private void recordFailure(ExecutionRecord record, String reason) {
        ExecutionRecord failed = record.withStatus("FAILED", reason);
        try {
            DatabaseLoader.applyDeviceActionAndLog(failed, null);
        } catch (RuntimeException ignored) {
            // SQL Server may be down. Preserve the failure in the local audit list.
        }
        appliedLog.add(failed);
    }

    public synchronized Map<Integer, Double> getSensorStates() { return ruleEngine.getSensorStates(); }
    public synchronized List<Rule> getRulesBySensor(int id) { return ruleEngine.getRulesBySensor(id); }
    public synchronized Map<Integer, List<Integer>> getDependencyGraph() { return ruleEngine.getDependencyGraphAsMap(); }
    public synchronized Map<String, Integer> getProcessingSummary() { return ruleEngine.getProcessingSummary(); }
    public synchronized void addDependency(int from, int to) { ruleEngine.addDependency(from, to); }

    public synchronized Rule addRule(CreateRuleRequest request) {
        int ruleId = DatabaseLoader.saveRule(request);
        Rule rule = new Rule(ruleId, request.name.trim());
        for (var c : request.conditions) {
            rule.addCondition(new Condition(c.sensorId, ComparisonOperator.fromSymbol(c.operator), c.value));
        }
        for (var a : request.actions) rule.addAction(new Action(a.deviceId, a.command.trim()));
        ruleEngine.addRule(rule);
        return rule;
    }

    public synchronized void enableRule(int id) {
        if (!ruleEngine.getRules().containsKey(id)) throw new NoSuchElementException("Rule not found");
        DatabaseLoader.updateRuleStatus(id, "active");
        ruleEngine.enableRule(id);
    }
    public synchronized void disableRule(int id) {
        if (!ruleEngine.getRules().containsKey(id)) throw new NoSuchElementException("Rule not found");
        DatabaseLoader.updateRuleStatus(id, "inactive");
        ruleEngine.disableRule(id);
    }

    public synchronized List<RulePreviewResponse> previewRules(int sensorId, double value) {
        List<RulePreviewResponse> results = new ArrayList<>();
        for (ExecutionRecord record : ruleEngine.preview(sensorId, value)) {
            Device device = devices.get(record.getDeviceId());
            String name = device != null ? device.getName() : "Device " + record.getDeviceId();
            results.add(new RulePreviewResponse(record.getRuleName(),
                    name + " would execute " + record.getActionCommand() + " (simulation)"));
        }
        return results;
    }
}
