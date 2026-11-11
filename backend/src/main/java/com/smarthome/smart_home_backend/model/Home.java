package com.smarthome.smart_home_backend.model;

public class Home {
    private int id;
    private String name;
    private String address;
    private String timezone;
    private String accessRole;

    public Home(int id, String name, String address, String timezone, String accessRole) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.timezone = timezone;
        this.accessRole = accessRole;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getTimezone() { return timezone; }
    public String getAccessRole() { return accessRole; }
}