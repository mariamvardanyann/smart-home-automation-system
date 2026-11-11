package com.smarthome.smart_home_backend.ds.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Rule {
    private final int ruleId;
    private final String name;
    private final List<Condition> conditions;
    private final List<Action> actions;
    private boolean enabled;

    public Rule(int ruleId, String name) {
        this.ruleId = ruleId;
        this.name = name;
        this.conditions = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.enabled = true;
    }

    public int getRuleId() {
        return ruleId;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public void addCondition(Condition condition) {
        conditions.add(condition);
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public List<Condition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public List<Action> getActions() {
        return Collections.unmodifiableList(actions);
    }

    /** Conservative single-reading check; multi-sensor rules require RuleEngine state. */
    public boolean matchesEvent(SensorEvent event) {
        return enabled && !conditions.isEmpty() && conditions.stream()
                .allMatch(c -> c.getSensorId() == event.getSensorId() && c.isSatisfied(event.getValue()));
    }

    @Override
    public String toString() {
        return "Rule{" +
                "ruleId=" + ruleId +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                ", conditions=" + conditions +
                ", actions=" + actions +
                '}';
    }
}