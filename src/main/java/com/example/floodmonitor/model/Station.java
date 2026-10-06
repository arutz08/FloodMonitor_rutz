package com.example.floodmonitor.model;

public class Station {

    private String id;
    private String stationName;
    private String river;
    private double warningThreshold = 2.5;
    private double criticalThreshold = 4.0;
    private StationStatus status = StationStatus.ACTIVE;
    private WarningLevel warningLevel = WarningLevel.UNKNOWN;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getRiver() {
        return river;
    }

    public void setRiver(String river) {
        this.river = river;
    }

    public double getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(double warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public double getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(double criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        this.status = status;
    }

    public WarningLevel getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(WarningLevel warningLevel) {
        this.warningLevel = warningLevel;
    }
}