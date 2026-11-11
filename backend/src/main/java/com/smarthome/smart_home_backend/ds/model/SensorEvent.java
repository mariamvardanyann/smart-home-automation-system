package com.smarthome.smart_home_backend.ds.model;

public class SensorEvent {
    private final int sensorId;
    private final double value;
    private final long timestamp;
    private final int eventId;

    public SensorEvent(int sensorId, double value) {
        this(sensorId, value, System.currentTimeMillis());
    }

    public SensorEvent(int sensorId, double value, long timestamp) {
        this(sensorId, value, timestamp, -1);
    }

    public SensorEvent(int sensorId, double value, long timestamp, int eventId) {
        this.sensorId = sensorId;
        this.value = value;
        this.timestamp = timestamp;
        this.eventId = eventId;
    }

    public int getSensorId() {
        return sensorId;
    }

    public double getValue() {
        return value;
    }

    public int getEventId() { return eventId; }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SensorEvent{" +
                "sensorId=" + sensorId +
                ", value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}