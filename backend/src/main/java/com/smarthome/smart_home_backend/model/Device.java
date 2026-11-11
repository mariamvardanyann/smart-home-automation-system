package com.smarthome.smart_home_backend.model;

public final class Device {
    private final int id;
    private final String name, room, type;
    private final boolean on, active;

    public Device(int id, String name, String room, String type, boolean on, boolean active) {
        this.id = id; this.name = name; this.room = room; this.type = type;
        this.on = on; this.active = active;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getRoom() { return room; }
    public String getType() { return type; }
    public boolean isOn() { return on; }
    public boolean isActive() { return active; }
}
