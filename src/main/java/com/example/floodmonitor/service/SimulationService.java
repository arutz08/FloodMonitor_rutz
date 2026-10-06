package com.example.floodmonitor.service;

import com.example.floodmonitor.model.*;
import com.example.floodmonitor.Repository.MeasurementRepository;
import com.example.floodmonitor.Repository.StationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.floodmonitor.exception.InvalidInputException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;

@Service
public class SimulationService {

    private static final String[] STATION_NAMES = {
            "Station Vienna", "Station Linz", "Station Salzburg",
            "Station Graz", "Station Innsbruck"
    };
    private static final String[] RIVERS = {"Donau", "Donau", "Salzach", "Mur", "Inn"};

    private static final double BASE_LEVEL = 1.5;
    private static final double FLOW_FACTOR = 12.0;
    private static final double LOW_BATTERY = 10.0;

    private final Random random = new Random();
    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;

    public SimulationService(StationRepository stationRepository,
                             MeasurementRepository measurementRepository) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
    }

    @Value("${simulation.interval:10000}")
    private long intervalMs;

    // Kommentar: Neue Hilfsmethoden
    public Measurement getLatestMeasurement(Station s) {
        return measurementRepository.findLatest(s.getId()).orElse(null);
    }

    public List<Measurement> getMeasurements(Station s) {
        return measurementRepository.findByStationId(s.getId());
    }

    @PostConstruct
    void init() {
        for (Station s : generateStations()) {
            stationRepository.save(s);
            s.setWarningLevel(calculateWarningLevel(s));
        }
    }

    // ---------- Simulation ----------

    @Scheduled(fixedRateString = "${simulation.interval:10000}")
    public void simulate() {
        for (Station s : stationRepository.findAll()) {
            switch (s.getStatus()) {
                case ACTIVE -> simulateMeasurement(s);
                case MAINTENANCE -> performMaintenance(s);
                default -> { }
            }
            s.setWarningLevel(calculateWarningLevel(s));
        }
    }

    private void simulateMeasurement(Station s) {
        Measurement prev = getLatestMeasurement(s);
        if (prev == null) return;

        double rain = prev.precipitation() * 0.9;
        if (random.nextDouble() < 0.05) {
            rain += 5 + random.nextDouble() * 15;
        }
        rain = round(Math.min(rain, 30.0), 1);

        double level = prev.waterLevel();
        level += rain * 0.02 - (level - BASE_LEVEL) * 0.05 + random.nextGaussian() * 0.03;
        level = round(Math.max(0.0, level), 2);

        double flow = round(FLOW_FACTOR * Math.pow(level, 1.5), 1);
        double temp = round(clamp(prev.waterTemperature() + random.nextGaussian() * 0.1, 0.0, 30.0), 1);
        double battery = round(clamp(prev.batteryLevel() - (0.2 + random.nextDouble() * 0.6), 0.0, 100.0), 1);

        measurementRepository.add(s.getId(), new Measurement(Instant.now(), level, temp, flow, rain, battery,
                levelFor(s, level, rain)));

        if (battery < LOW_BATTERY) {
            s.setStatus(StationStatus.MAINTENANCE);
        }
    }

    private void performMaintenance(Station s) {
        Measurement prev = getLatestMeasurement(s);
        if (prev == null) return;
        double battery = round(Math.min(100.0, prev.batteryLevel() + 20.0), 1);
        measurementRepository.add(s.getId(), new Measurement(Instant.now(), prev.waterLevel(), prev.waterTemperature(),
                prev.waterFlow(), prev.precipitation(), battery, WarningLevel.UNKNOWN));
        if (battery >= 80.0) {
            s.setStatus(StationStatus.ACTIVE);
        }
    }

    // ---------- Warnstufe ----------

    /** Reine Wertebewertung (für jede einzelne Messung). */
    private WarningLevel levelFor(Station s, double waterLevel, double precipitation) {
        if (waterLevel < 0) return WarningLevel.UNKNOWN;
        if (waterLevel >= s.getCriticalThreshold()) return WarningLevel.CRITICAL;
        if (waterLevel >= s.getWarningThreshold()) return WarningLevel.WARNING;
        if (precipitation >= 15.0 && waterLevel >= 0.8 * s.getWarningThreshold()) {
            return WarningLevel.WARNING;
        }
        return WarningLevel.NORMAL;
    }

    /** Aktuelle Warnstufe der Station. */
    public WarningLevel calculateWarningLevel(Station s) {
        Measurement m = getLatestMeasurement(s);
        if (s.getStatus() != StationStatus.ACTIVE || !isCurrent(m)) {
            return WarningLevel.UNKNOWN;
        }
        return levelFor(s, m.waterLevel(), m.precipitation());
    }

    private boolean isCurrent(Measurement m) {
        return m != null && m.timestamp() != null
                && Duration.between(m.timestamp(), Instant.now()).toMillis() <= 3 * intervalMs;
    }

    // ---------- Abfragen ----------

    public List<Station> getStations() {
        return stationRepository.findAll();
    }

    public List<Station> filterStations(String river, Boolean isActive, WarningLevel level) {
        return stationRepository.findAll().stream()
                .filter(s -> river == null || river.equalsIgnoreCase(s.getRiver()))
                .filter(s -> isActive == null || (s.getStatus() == StationStatus.ACTIVE) == isActive)
                .filter(s -> level == null || s.getWarningLevel() == level)
                .toList();
    }

    // Kommentar: Validierungs-Hilfsmethode für Filterparameter
    private void validateMeasurementFilterParams(Instant from, Instant to, Integer limit) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidInputException("Startzeitpunkt (from) darf nicht nach dem Endzeitpunkt (to) liegen");
        }
        if (limit != null && limit < 0) {
            throw new InvalidInputException("Das Limit darf nicht negativ sein");
        }
    }

    public List<Measurement> filterMeasurements(Station s, Instant from, Instant to,
                                                WarningLevel level, Integer limit) {
        // Kommentar: Erst Parameter validieren
        validateMeasurementFilterParams(from, to, limit);

        List<Measurement> result = measurementRepository.findByStationId(s.getId()).stream()
                .filter(m -> from == null || !m.timestamp().isBefore(from))
                .filter(m -> to == null || !m.timestamp().isAfter(to))
                .filter(m -> level == null || m.warningLevel() == level)
                .toList();
        // limit: die neuesten N Einträge
        if (limit != null && limit < result.size()) {
            result = result.subList(result.size() - limit, result.size());
        }
        return new ArrayList<>(result);
    }

    public Statistics calculateStatistics(Station s, Instant from, Instant to) {
        List<Measurement> list = filterMeasurements(s, from, to, null, null);
        if (list.isEmpty()) {
            return new Statistics(s.getId(), 0, 0, 0, 0, 0, 0, 0, 0);
        }
        DoubleSummaryStatistics level = list.stream().mapToDouble(Measurement::waterLevel).summaryStatistics();
        double avgFlow = list.stream().mapToDouble(Measurement::waterFlow).average().orElse(0);
        double rainSum = list.stream().mapToDouble(Measurement::precipitation).sum();
        long critical = list.stream().filter(m -> m.warningLevel() == WarningLevel.CRITICAL).count();
        long warnings = list.stream().filter(m -> m.warningLevel() == WarningLevel.WARNING).count() + critical;

        return new Statistics(s.getId(), list.size(),
                level.getMin(), level.getMax(), round(level.getAverage(), 2),
                round(avgFlow, 1), round(rainSum, 1), warnings, critical);
    }

    public boolean existsById(String id) {
        return stationRepository.existsById(id);
    }

    public Station findById(String id) {
        return stationRepository.findById(id).orElse(null);
    }

    public Station addStation(Station station) {
        if (station.getStatus() == null) station.setStatus(StationStatus.ACTIVE);
        station.setWarningLevel(calculateWarningLevel(station));
        return stationRepository.save(station);
    }

    public Station setStatus(String id, StationStatus status) {
        Station s = findById(id);
        if (s != null) {
            s.setStatus(status);
            s.setWarningLevel(calculateWarningLevel(s));
        }
        return s;
    }

    public Station setThresholds(String id, double warning, double critical) {
        Station s = findById(id);
        if (s != null) {
            s.setWarningThreshold(warning);
            s.setCriticalThreshold(critical);
            s.setWarningLevel(calculateWarningLevel(s));
        }
        return s;
    }

    public int getCount() {
        return stationRepository.count();
    }

    // ---------- Startdaten ----------

    public List<Station> generateStations() {
        List<Station> generated = new ArrayList<>();
        for (int i = 0; i < STATION_NAMES.length; i++) {
            Station s = new Station();
            s.setId(String.valueOf(i + 1));
            s.setStationName(STATION_NAMES[i]);
            s.setRiver(RIVERS[i]);

            double level = round(0.5 + random.nextDouble() * 2.0, 2);
            measurementRepository.add(s.getId(), new Measurement(
                    Instant.now(),
                    level,
                    round(4.0 + random.nextDouble() * 16.0, 1),
                    round(FLOW_FACTOR * Math.pow(level, 1.5), 1),
                    0.0,
                    round(60.0 + random.nextDouble() * 40.0, 1),
                    levelFor(s, level, 0.0)));
            generated.add(s);
        }
        return generated;
    }

    // Kommentar: Statische Mathe-Hilfsmethoden
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}