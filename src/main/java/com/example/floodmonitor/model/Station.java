package com.example.floodmonitor.model;

public class Station {
    private String id;
    private String stationName;
    private String river;
    private String unit = "m";
    private StationStatus status = StationStatus.ACTIVE;
    private double warningThreshold = 3.0;
    private double criticalThreshold = 4.5;
    private WarningLevel warningLevel = WarningLevel.UNKNOWN;

    public Station() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getRiver() { return river; }
    public void setRiver(String river) { this.river = river; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public StationStatus getStatus() { return status; }
    public void setStatus(StationStatus status) { this.status = status; }
    public double getWarningThreshold() { return warningThreshold; }
    public void setWarningThreshold(double warningThreshold) { this.warningThreshold = warningThreshold; }
    public double getCriticalThreshold() { return criticalThreshold; }
    public void setCriticalThreshold(double criticalThreshold) { this.criticalThreshold = criticalThreshold; }
    public WarningLevel getWarningLevel() { return warningLevel; }
    public void setWarningLevel(WarningLevel warningLevel) { this.warningLevel = warningLevel; }
}