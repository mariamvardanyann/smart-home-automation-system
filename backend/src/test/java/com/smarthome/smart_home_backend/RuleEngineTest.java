package com.smarthome.smart_home_backend;

import com.smarthome.smart_home_backend.ds.engine.RuleEngine;
import com.smarthome.smart_home_backend.ds.model.*;
import com.smarthome.smart_home_backend.ds.structures.EventQueue;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests: intentionally do not require SQL Server or start Spring. */
class RuleEngineTest {
    private Rule rule(int id, int sensorId, ComparisonOperator op, double threshold, int deviceId) {
        Rule rule = new Rule(id, "Rule " + id);
        rule.addCondition(new Condition(sensorId, op, threshold));
        rule.addAction(new Action(deviceId, "turn_on"));
        return rule;
    }

    @Test void queueSizeTracksEnqueueAndDequeue() {
        EventQueue queue = new EventQueue();
        assertEquals(0, queue.size());
        queue.enqueue(new SensorEvent(1, 5));
        queue.enqueue(new SensorEvent(2, 6));
        assertEquals(2, queue.size());
        assertEquals(1, queue.dequeue().getSensorId());
        assertEquals(1, queue.size());
        queue.dequeue();
        assertEquals(0, queue.size());
    }

    @Test void multipleConditionsMustAllMatch() {
        RuleEngine engine = new RuleEngine();
        Rule rule = rule(1, 1, ComparisonOperator.EQUAL, 1, 10);
        rule.addCondition(new Condition(2, ComparisonOperator.LESS_THAN, 20));
        engine.addRule(rule);
        engine.triggerEvent(1, 1);
        engine.processAllEvents();
        assertEquals(0, engine.getExecutionLog().size());
        engine.triggerEvent(2, 18);
        engine.processAllEvents();
        assertEquals(1, engine.getExecutionLog().size());
        assertEquals("PLANNED", engine.getExecutionLog().get(0).getStatus());
    }

    @Test void previewIsAccurateAndSideEffectFree() {
        RuleEngine engine = new RuleEngine();
        Rule rule = rule(1, 1, ComparisonOperator.EQUAL, 1, 10);
        rule.addCondition(new Condition(2, ComparisonOperator.LESS_THAN, 20));
        engine.addRule(rule);
        assertEquals(0, engine.preview(1, 1).size());
        engine.triggerEvent(2, 18);
        engine.processAllEvents();
        assertEquals(1, engine.preview(1, 1).size());
        assertFalse(engine.getSensorStates().containsKey(1));
        assertEquals(0, engine.getExecutionLog().size());
        assertEquals(1, engine.getProcessingSummary().get("totalEventsProcessed").intValue());
    }

    @Test void dependenciesRejectCycles() {
        RuleEngine engine = new RuleEngine();
        engine.addRule(rule(1, 1, ComparisonOperator.EQUAL, 1, 10));
        engine.addRule(rule(2, 1, ComparisonOperator.EQUAL, 1, 20));
        engine.addDependency(1, 2);
        assertThrows(IllegalArgumentException.class, () -> engine.addDependency(2, 1));
        assertFalse(engine.hasCycle());
        assertEquals(List.of(2), engine.getDependencyGraphAsMap().get(1));
    }

    @Test void sharedDependencyDoesNotDoubleExecute() {
        RuleEngine engine = new RuleEngine();
        engine.addRule(rule(1, 1, ComparisonOperator.EQUAL, 1, 10));
        engine.addRule(rule(2, 1, ComparisonOperator.EQUAL, 1, 20));
        engine.addRule(rule(3, 1, ComparisonOperator.EQUAL, 1, 30));
        engine.addDependency(1, 3);
        engine.addDependency(2, 3);
        engine.triggerEvent(1, 1);
        engine.processAllEvents();
        assertEquals(3, engine.getExecutionLog().size());
        assertEquals(3, engine.getProcessingSummary().get("totalRulesFired").intValue());
    }

    @Test void invalidEventIsRejectedAndEqualityAliasesWork() {
        RuleEngine engine = new RuleEngine();
        assertThrows(IllegalArgumentException.class, () -> engine.triggerEvent(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> engine.preview(1, Double.NaN));
        assertEquals(ComparisonOperator.EQUAL, ComparisonOperator.fromSymbol("="));
        assertEquals(ComparisonOperator.NOT_EQUAL, ComparisonOperator.fromSymbol("<>"));
    }

    @Test void disabledRuleNeverFires() {
        RuleEngine engine = new RuleEngine();
        engine.addRule(rule(1, 1, ComparisonOperator.EQUAL, 1, 10));
        engine.disableRule(1);
        assertTrue(engine.preview(1, 1).isEmpty());
        engine.triggerEvent(1, 1);
        engine.processAllEvents();
        assertEquals(0, engine.getExecutionLog().size());
    }
}
