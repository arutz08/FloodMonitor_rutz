package com.example.floodmonitor.model;

public class Station {
    private String id;
    private String stationName;
    private String timestamp;
    private double waterLevel;
    private double waterTemperature;
    private String unit;
    private double waterFlow;

    public Station() {}

    public Station(String id, String stationName, String timestamp, double waterLevel, double waterTemperature, String unit, double waterFlow) {
        this.id = id;
        this.stationName = stationName;
        this.timestamp = timestamp;
        this.waterLevel = waterLevel;
        this.waterTemperature = waterTemperature;
        this.unit = unit;
        this.waterFlow = waterFlow;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(double waterLevel) { this.waterLevel = waterLevel; }
    public double getWaterTemperature() { return waterTemperature; }
    public void setWaterTemperature(double waterTemperature) { this.waterTemperature = waterTemperature; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getWaterFlow() {
        return waterFlow;
    }

    public void setWaterFlow(double waterFlow) {
        this.waterFlow = waterFlow;
    }
}
