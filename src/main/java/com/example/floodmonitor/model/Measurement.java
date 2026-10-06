package com.example.floodmonitor.model;

import java.time.Instant;

public record Measurement(
        Instant timestamp,
        double waterLevel,
        double waterTemperature,
        double waterFlow,
        double precipitation,
        double batteryLevel,
        WarningLevel warningLevel
) {}