package com.smarthome.smart_home_backend.ds.structures;

import java.util.*;

public class RuleGraph {
    private final Map<Integer, List<Integer>> adjacencyList;

    public RuleGraph() {
        this.adjacencyList = new HashMap<>();
    }

    public void addRule(int ruleId) {
        adjacencyList.putIfAbsent(ruleId, new ArrayList<>());
    }

    public void addDependency(int fromRuleId, int toRuleId) {
        addRule(fromRuleId);
        addRule(toRuleId);

        List<Integer> neighbors = adjacencyList.get(fromRuleId);
        if (!neighbors.contains(toRuleId)) {
            neighbors.add(toRuleId);
        }
    }

    public void removeDependency(int fromRuleId, int toRuleId) {
        if (!adjacencyList.containsKey(fromRuleId)) {
            return;
        }

        adjacencyList.get(fromRuleId).remove(Integer.valueOf(toRuleId));
    }

    public void removeRule(int ruleId) {
        adjacencyList.remove(ruleId);

        for (List<Integer> neighbors : adjacencyList.values()) {
            neighbors.remove(Integer.valueOf(ruleId));
        }
    }

    public List<Integer> getDependencies(int ruleId) {
        return adjacencyList.getOrDefault(ruleId, new ArrayList<>());
    }

    public boolean containsRule(int ruleId) {
        return adjacencyList.containsKey(ruleId);
    }

    public void printGraph() {
        if (adjacencyList.isEmpty()) {
            System.out.println("Dependency graph is empty.");
            return;
        }

        System.out.println("Rule Dependency Graph:");
        for (Map.Entry<Integer, List<Integer>> entry : adjacencyList.entrySet()) {
            System.out.println("Rule " + entry.getKey() + " -> " + entry.getValue());
        }
    }

    public boolean hasCycle() {
        Set<Integer> visited = new HashSet<>();
        Set<Integer> recursionStack = new HashSet<>();

        for (Integer ruleId : adjacencyList.keySet()) {
            if (!visited.contains(ruleId)) {
                if (dfsCycleCheck(ruleId, visited, recursionStack)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean dfsCycleCheck(int currentRule,
                                  Set<Integer> visited,
                                  Set<Integer> recursionStack) {
        visited.add(currentRule);
        recursionStack.add(currentRule);

        for (Integer neighbor : adjacencyList.getOrDefault(currentRule, new ArrayList<>())) {
            if (!visited.contains(neighbor)) {
                if (dfsCycleCheck(neighbor, visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack.contains(neighbor)) {
                return true;
            }
        }

        recursionStack.remove(currentRule);
        return false;
    }

    public List<Integer> getAllRules() {
        return new ArrayList<>(adjacencyList.keySet());
    }

    public Map<Integer, List<Integer>> getAdjacencyList() {
        Map<Integer, List<Integer>> copy = new HashMap<>();

        for (Map.Entry<Integer, List<Integer>> entry : adjacencyList.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }
}
