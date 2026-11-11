package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.ds.model.ExecutionRecord;
import com.smarthome.smart_home_backend.ds.model.Rule;
import com.smarthome.smart_home_backend.dto.CreateRuleRequest;
import com.smarthome.smart_home_backend.dto.RulePreviewResponse;
import com.smarthome.smart_home_backend.facade.AutomationFacade;
import com.smarthome.smart_home_backend.model.Device;
import org.springframework.stereotype.Service;
import java.util.Map;

import java.util.List;

@Service
public class DeviceService {

    private final AutomationFacade automationFacade;

    public DeviceService(AutomationFacade automationFacade) {
        this.automationFacade = automationFacade;
    }

    public List<Device> getDevices() {
        return automationFacade.getAllDevices();
    }

    public Device toggleDevice(int id) {
        return automationFacade.toggleDevice(id);
    }

    public String getRulesDebug() {
        return automationFacade.getRulesDebug();
    }

    public List<Rule> getAllRules() {
        return automationFacade.getAllRules();
    }


    public void enableRule(int id) {
        automationFacade.enableRule(id);
    }

    public void disableRule(int id) {
        automationFacade.disableRule(id);
    }

    public List<ExecutionRecord> getExecutionLog() {
        return automationFacade.getExecutionLog();
    }

    public void triggerEvent(int sensorId, double value) {
        automationFacade.triggerEvent(sensorId, value);
    }

    public void processAllEvents() {
        automationFacade.processAllEvents();
    }

    public Map<Integer, Double> getSensorStates() {
        return automationFacade.getSensorStates();
    }

    public List<Rule> getRulesBySensor(int sensorId) {
        return automationFacade.getRulesBySensor(sensorId);
    }

    public Map<Integer, List<Integer>> getDependencyGraph() {
        return automationFacade.getDependencyGraph();
    }

    public Map<String, Integer> getProcessingSummary() {
        return automationFacade.getProcessingSummary();
    }
    public void addDependency(int fromRuleId, int toRuleId) {
        automationFacade.addDependency(fromRuleId, toRuleId);
    }

    public Rule addRule(CreateRuleRequest request) {
        return automationFacade.addRule(request);
    }

    public List<RulePreviewResponse> previewRules(int sensorId, double value) {
        return automationFacade.previewRules(sensorId, value);
    }

}