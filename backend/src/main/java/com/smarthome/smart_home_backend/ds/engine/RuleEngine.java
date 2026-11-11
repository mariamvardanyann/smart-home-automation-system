package com.smarthome.smart_home_backend.ds.engine;

import com.smarthome.smart_home_backend.ds.model.*;
import com.smarthome.smart_home_backend.ds.structures.*;

import java.util.*;

/** Pure, in-memory rule planner. Device actions are applied by AutomationFacade. */
public class RuleEngine {
    private final Map<Integer, Rule> rules = new LinkedHashMap<>();
    private SensorTrie sensorTrie = new SensorTrie();
    private final EventQueue eventQueue = new EventQueue();
    private final RuleGraph ruleGraph = new RuleGraph();
    private final List<ExecutionRecord> executionLog = new ArrayList<>();
    private final Map<Integer, Double> sensorStates = new HashMap<>();
    private int totalEventsProcessed, totalRulesFired, totalActionsExecuted;
    private int totalUnknownSensorEvents, totalUnmatchedEvents;

    public synchronized void addRule(Rule rule) {
        Objects.requireNonNull(rule, "rule");
        if (rule.getRuleId() <= 0 || rules.containsKey(rule.getRuleId())) {
            throw new IllegalArgumentException("Invalid or duplicate rule ID: " + rule.getRuleId());
        }
        if (rule.getConditions().isEmpty() || rule.getActions().isEmpty()) {
            throw new IllegalArgumentException("Each rule needs at least one condition and one action");
        }
        for (Condition condition : rule.getConditions()) {
            if (condition.getSensorId() <= 0 || !Double.isFinite(condition.getThreshold())) {
                throw new IllegalArgumentException("Invalid sensor ID or threshold in rule " + rule.getRuleId());
            }
        }
        rules.put(rule.getRuleId(), rule);
        sensorTrie.insertRule(rule);
        ruleGraph.addRule(rule.getRuleId());
    }

    public synchronized void addRules(List<Rule> input) { for (Rule rule : input) addRule(rule); }

    public synchronized void deleteRule(int ruleId) {
        if (rules.remove(ruleId) == null) throw new NoSuchElementException("Rule not found: " + ruleId);
        ruleGraph.removeRule(ruleId);
        sensorTrie = new SensorTrie();
        rules.values().forEach(sensorTrie::insertRule);
    }

    public synchronized void enableRule(int id) { findRule(id).enable(); }
    public synchronized void disableRule(int id) { findRule(id).disable(); }

    private Rule findRule(int id) {
        Rule rule = rules.get(id);
        if (rule == null) throw new NoSuchElementException("Rule not found: " + id);
        return rule;
    }

    public synchronized void addDependency(int fromRuleId, int toRuleId) {
        findRule(fromRuleId);
        findRule(toRuleId);
        if (fromRuleId == toRuleId) throw new IllegalArgumentException("Self-dependency is forbidden");
        // A duplicate edge is harmless. A new cycle is rejected and rolled back.
        ruleGraph.addDependency(fromRuleId, toRuleId);
        if (ruleGraph.hasCycle()) {
            ruleGraph.removeDependency(fromRuleId, toRuleId);
            throw new IllegalArgumentException("Dependency would create a cycle");
        }
    }

    public synchronized void triggerEvent(int sensorId, double value) {
        triggerEvent(sensorId, value, -1);
    }

    public synchronized void triggerEvent(int sensorId, double value, int databaseEventId) {
        if (sensorId <= 0 || !Double.isFinite(value)) {
            throw new IllegalArgumentException("Sensor ID must be positive and reading finite");
        }
        eventQueue.enqueue(new SensorEvent(sensorId, value, System.currentTimeMillis(), databaseEventId));
    }

    public synchronized void processNextEvent() {
        if (eventQueue.isEmpty()) return;
        SensorEvent event = eventQueue.dequeue();
        totalEventsProcessed++;
        sensorStates.put(event.getSensorId(), event.getValue());
        List<Rule> candidates = sensorTrie.getRulesBySensorId(event.getSensorId());
        if (candidates.isEmpty()) {
            totalUnknownSensorEvents++;
            return;
        }
        List<ExecutionRecord> planned = plan(event, sensorStates);
        if (planned.isEmpty()) totalUnmatchedEvents++;
        for (ExecutionRecord record : planned) executionLog.add(record);
        totalActionsExecuted += planned.size();
        totalRulesFired += planned.stream().map(ExecutionRecord::getRuleId).distinct().count();
    }

    public synchronized void processAllEvents() {
        while (!eventQueue.isEmpty()) processNextEvent();
    }

    /** Preview never mutates sensor readings, event queue, counters or log. */
    public synchronized List<ExecutionRecord> preview(int sensorId, double value) {
        if (sensorId <= 0 || !Double.isFinite(value)) {
            throw new IllegalArgumentException("Sensor ID must be positive and reading finite");
        }
        Map<Integer, Double> candidateStates = new HashMap<>(sensorStates);
        candidateStates.put(sensorId, value);
        return List.copyOf(plan(new SensorEvent(sensorId, value), candidateStates));
    }

    private List<ExecutionRecord> plan(SensorEvent event, Map<Integer, Double> states) {
        List<ExecutionRecord> planned = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        for (Rule rule : sensorTrie.getRulesBySensorId(event.getSensorId())) {
            planRule(rule, event, states, visited, planned);
        }
        return planned;
    }

    private void planRule(Rule rule, SensorEvent event, Map<Integer, Double> states,
                          Set<Integer> visited, List<ExecutionRecord> planned) {
        if (!visited.add(rule.getRuleId()) || !matchesAllConditions(rule, states)) return;
        // A matched rule with no actions should not exist after validation.
        for (Action action : rule.getActions()) {
            planned.add(new ExecutionRecord(rule.getRuleId(), rule.getName(), event.getSensorId(),
                    event.getValue(), action.getCommand(), action.getDeviceId(), event.getTimestamp(),
                    "PLANNED", event.getEventId(), null));
        }
        for (Integer nextId : ruleGraph.getDependencies(rule.getRuleId())) {
            Rule next = rules.get(nextId);
            if (next != null) planRule(next, event, states, visited, planned);
        }
    }

    private boolean matchesAllConditions(Rule rule, Map<Integer, Double> states) {
        if (!rule.isEnabled() || rule.getConditions().isEmpty()) return false;
        for (Condition condition : rule.getConditions()) {
            Double current = states.get(condition.getSensorId());
            if (current == null || !condition.isSatisfied(current)) return false;
        }
        return true;
    }

    public synchronized Map<Integer, Rule> getRules() { return Collections.unmodifiableMap(new LinkedHashMap<>(rules)); }
    public synchronized List<ExecutionRecord> getExecutionLog() { return List.copyOf(executionLog); }
    public synchronized Map<Integer, Double> getSensorStates() { return Map.copyOf(sensorStates); }
    public synchronized List<Rule> getRulesBySensor(int sensorId) { return List.copyOf(sensorTrie.getRulesBySensorId(sensorId)); }
    public synchronized Map<Integer, List<Integer>> getDependencyGraphAsMap() { return ruleGraph.getAdjacencyList(); }
    public synchronized boolean hasCycle() { return ruleGraph.hasCycle(); }
    public synchronized int getPendingEventCount() { return eventQueue.size(); }
    public synchronized Map<String, Integer> getProcessingSummary() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("totalEventsProcessed", totalEventsProcessed);
        summary.put("totalRulesFired", totalRulesFired);
        summary.put("totalActionsExecuted", totalActionsExecuted);
        summary.put("totalUnknownSensorEvents", totalUnknownSensorEvents);
        summary.put("totalUnmatchedEvents", totalUnmatchedEvents);
        summary.put("pendingEvents", eventQueue.size());
        return summary;
    }
}
