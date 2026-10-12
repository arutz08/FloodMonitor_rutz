package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.Statistics;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.service.SimulationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StationController {

    private static final int MAX_LIMIT = 1000;

    private final SimulationService simulationService;

    public StationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    // ---------- Stationen ----------

    @GetMapping(value = "/stations",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<Station> getStations(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String river,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(name = "warningLevel", required = false) WarningLevel warningLevel) {
        List<Station> result = simulationService.filterStations(river, isActive, warningLevel).stream()
                .filter(s -> id == null || s.getId().equals(id))
                .toList();
        if (id != null && result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Station mit der ID " + id + " existiert nicht");
        }
        return result;
    }

    @GetMapping(value = "/stations/{stationId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Station getStation(@PathVariable String stationId) {
        return requireStation(stationId);
    }

    @PostMapping(value = "/stations",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Station> addStation(@RequestBody Station station) {
        if (simulationService.existsById(station.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Station mit der ID " + station.getId() + " existiert bereits");
        }
        simulationService.addStation(station);
        return ResponseEntity.status(HttpStatus.CREATED).body(station);
    }

    @PatchMapping("/stations/{id}/status")
    public Station setStatus(@PathVariable String id, @RequestParam StationStatus status) {
        Station s = simulationService.setStatus(id, status);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Station mit der ID " + id + " existiert nicht");
        }
        return s;
    }

    @PatchMapping("/stations/{id}/thresholds")
    public Station setThresholds(@PathVariable String id,
                                 @RequestParam double warning,
                                 @RequestParam double critical) {
        Station s = simulationService.setThresholds(id, warning, critical);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Station mit der ID " + id + " existiert nicht");
        }
        return s;
    }

    @GetMapping(value = "/count",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public int getCount() {
        return simulationService.getCount();
    }

    // ---------- Messwerte ----------

    @GetMapping(value = "/stations/{stationId}/measurements/latest",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Measurement getLatest(@PathVariable String stationId) {
        Station s = requireStation(stationId);
        Measurement m = simulationService.getLatestMeasurement(s);
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Station " + stationId + " hat noch keine Messung");
        }
        return m;
    }

    @GetMapping(value = "/stations/{stationId}/measurements",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<Measurement> getMeasurements(
            @PathVariable String stationId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(name = "warningLevel", required = false) WarningLevel warningLevel,
            @RequestParam(required = false) Integer limit) {
        Station s = requireStation(stationId);

        if (limit != null && (limit < 1 || limit > MAX_LIMIT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "limit muss zwischen 1 und " + MAX_LIMIT + " liegen");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'from' darf nicht nach 'to' liegen");
        }
        return simulationService.filterMeasurements(s, from, to, warningLevel, limit);
    }

    @GetMapping(value = "/stations/{stationId}/statistics",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Statistics getStatistics(@PathVariable String stationId,
                                    @RequestParam(required = false) Instant from,
                                    @RequestParam(required = false) Instant to) {
        Station s = requireStation(stationId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'from' darf nicht nach 'to' liegen");
        }
        return simulationService.calculateStatistics(s, from, to);
    }

    // ---------- Warnungen ----------

    @GetMapping(value = "/alerts",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<Station> getAlerts(
            @RequestParam(name = "warningLevel", required = false) WarningLevel warningLevel,
            @RequestParam(required = false) String river) {
        return simulationService.filterStations(river, null, warningLevel).stream()
                .filter(s -> warningLevel != null
                        || s.getWarningLevel() == WarningLevel.WARNING
                        || s.getWarningLevel() == WarningLevel.CRITICAL)
                .toList();
    }

    // ---------- Hilfsmethode ----------

    private Station requireStation(String stationId) {
        Station s = simulationService.findById(stationId);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Station mit der ID " + stationId + " existiert nicht");
        }
        return s;
    }
}