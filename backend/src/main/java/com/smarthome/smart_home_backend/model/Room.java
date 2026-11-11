package com.smarthome.smart_home_backend.model;

public class Room {
    private int id;
    private int homeId;
    private String name;
    private int floor;

    public Room(int id, int homeId, String name, int floor) {
        this.id = id;
        this.homeId = homeId;
        this.name = name;
        this.floor = floor;
    }

    public int getId() { return id; }
    public int getHomeId() { return homeId; }
    public String getName() { return name; }
    public int getFloor() { return floor; }
}