package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.db.DatabaseLoader;
import com.smarthome.smart_home_backend.ds.model.ExecutionRecord;
import com.smarthome.smart_home_backend.ds.model.Rule;
import com.smarthome.smart_home_backend.dto.*;
import com.smarthome.smart_home_backend.model.*;
import com.smarthome.smart_home_backend.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Prototype API. Protected with a single demo API key, NOT per-user authorization. */
@RestController
public class DeviceController {
    private final DeviceService deviceService;
    public DeviceController(DeviceService deviceService) { this.deviceService = deviceService; }

    @GetMapping("/devices") public List<Device> getDevices() { return deviceService.getDevices(); }
    @PutMapping("/devices/{id}/toggle") public Device toggleDevice(@PathVariable int id) { return deviceService.toggleDevice(id); }
    @GetMapping("/rules") public List<Rule> getRules() { return deviceService.getAllRules(); }
    @PutMapping("/rules/{id}/enable") public Map<String, String> enableRule(@PathVariable int id) {
        deviceService.enableRule(id); return Map.of("status", "enabled");
    }
    @PutMapping("/rules/{id}/disable") public Map<String, String> disableRule(@PathVariable int id) {
        deviceService.disableRule(id); return Map.of("status", "disabled");
    }
    @GetMapping("/logs") public List<ExecutionRecord> getLogs() { return deviceService.getExecutionLog(); }
    @PostMapping("/events") public Map<String, String> triggerEvent(@RequestBody SensorReadingRequest request) {
        deviceService.triggerEvent(request.sensorId, request.value); return Map.of("status", "queued");
    }
    @PostMapping("/events/process") public Map<String, String> processEvents() {
        deviceService.processAllEvents(); return Map.of("status", "processed");
    }
    @GetMapping("/sensors") public Map<Integer, Double> getSensors() { return deviceService.getSensorStates(); }
    @GetMapping("/rules/by-sensor/{sensorId}") public List<Rule> getRulesBySensor(@PathVariable int sensorId) {
        return deviceService.getRulesBySensor(sensorId);
    }
    @GetMapping("/dependency-graph") public Map<Integer, List<Integer>> getDependencyGraph() {
        return deviceService.getDependencyGraph();
    }
    @GetMapping("/summary") public Map<String, Integer> getProcessingSummary() {
        return deviceService.getProcessingSummary();
    }
    @PostMapping("/dependencies") public Map<String, String> addDependency(@RequestBody DependencyRequest request) {
        deviceService.addDependency(request.fromRuleId, request.toRuleId);
        return Map.of("status", "added", "scope", "in-memory only");
    }
    @GetMapping("/users/{userId}/homes") public List<Home> getHomesByUser(@PathVariable int userId) {
        return DatabaseLoader.loadHomesByUser(userId);
    }
    @GetMapping("/homes/{homeId}/rooms") public List<Room> getRoomsByHome(@PathVariable int homeId) {
        return DatabaseLoader.loadRoomsByHome(homeId);
    }
    @GetMapping("/rooms/{roomId}/devices") public List<Device> getDevicesByRoom(@PathVariable int roomId) {
        return DatabaseLoader.loadDevicesByRoom(roomId);
    }
    @PostMapping("/rules") public Rule addRule(@RequestBody CreateRuleRequest request) {
        return deviceService.addRule(request);
    }
    @GetMapping("/logs/details") public List<ReadableLogResponse> getReadableLogs() {
        return DatabaseLoader.loadReadableLogs();
    }
    @GetMapping("/homes/{homeId}/logs/details") public List<ReadableLogResponse> getReadableLogsByHome(@PathVariable int homeId) {
        return DatabaseLoader.loadReadableLogsByHome(homeId);
    }
    @GetMapping("/homes/{homeId}/dashboard") public HomeDashboardResponse getHomeDashboard(@PathVariable int homeId) {
        return DatabaseLoader.loadHomeDashboard(homeId);
    }
    @PostMapping("/rules/preview") public List<RulePreviewResponse> previewRules(@RequestBody SensorReadingRequest request) {
        return deviceService.previewRules(request.sensorId, request.value);
    }
    @PostMapping("/devices/{id}/test") public ResponseEntity<Map<String, String>> testDevice(@PathVariable int id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("message",
                "Physical-device testing is not available; this is a database-backed simulator."));
    }
}
