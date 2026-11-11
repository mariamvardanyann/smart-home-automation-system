package com.smarthome.smart_home_backend.ds.structures;

import com.smarthome.smart_home_backend.ds.model.*;
import java.util.ArrayList;
import java.util.List;

public class TrieNode {
    private final TrieNode[] children;
    private final List<Rule> rulesAtNode;
    private boolean isEndOfSensorId;

    public TrieNode() {
        this.children = new TrieNode[10]; // digits 0-9
        this.rulesAtNode = new ArrayList<>();
        this.isEndOfSensorId = false;
    }

    public TrieNode[] getChildren() {
        return children;
    }

    public List<Rule> getRulesAtNode() {
        return rulesAtNode;
    }

    public boolean isEndOfSensorId() {
        return isEndOfSensorId;
    }

    public void setEndOfSensorId(boolean endOfSensorId) {
        isEndOfSensorId = endOfSensorId;
    }
}