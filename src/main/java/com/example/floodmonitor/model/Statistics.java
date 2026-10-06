package com.example.floodmonitor.model;

public record Statistics(
        String stationId,
        int measurementCount,
        double minWaterLevel,
        double maxWaterLevel,
        double avgWaterLevel,
        double avgWaterFlow,
        double totalPrecipitation,
        long warningCount,
        long criticalCount
) {}