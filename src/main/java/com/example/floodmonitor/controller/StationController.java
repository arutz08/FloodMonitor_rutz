package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.service.SimulationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StationController {

    private final SimulationService simulationService;

    public StationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping(value = "/stations", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<Station> getStations(@RequestParam(required = false) String id) {
        List<Station> stations = simulationService.getStations();
        if (id == null) {
            return stations;
        }
        List<Station> result = stations.stream()
                .filter(s -> s.getId().equals(id))
                .toList();

        if (result.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Station mit der ID " + id + " existiert nicht");
        }
        return result;
    }

    @PostMapping(value = "/stations",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Station> addStation(@RequestBody Station station){
        if (simulationService.existsById(station.getId())){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Station mit der ID " + station.getId() + " existiert bereits");
        }
        simulationService.addStation(station);
        return ResponseEntity.status(HttpStatus.CREATED).body(station);
    }

    @PatchMapping("/stations/{id}/status")
    public Station setStatus(@PathVariable String id, @RequestParam StationStatus status) {
        Station s = simulationService.setStatus(id, status);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Station mit der ID " + id + " existiert nicht");
        }
        return s;
    }

    @GetMapping(value = "/count", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public int getCount(){
        return simulationService.getCount();
    }

    @GetMapping(value = "/stations/{stationId}/measurements/latest",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Measurement getLatest(@PathVariable String stationId) {
        Station s = simulationService.findById(stationId);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Station mit der ID " + stationId + " existiert nicht");
        }
        // Kommentar: Aufruf auf simulationService angepasst
        Measurement m = simulationService.getLatestMeasurement(s);
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Station " + stationId + " hat noch keine Messung");
        }
        return m;
    }

    @GetMapping(value = "/stations/{stationId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Station getStation(@PathVariable String stationId) {
        Station s = simulationService.findById(stationId);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Station mit der ID " + stationId + " existiert nicht");
        }
        return s;
    }

    @GetMapping(value = "/stations/{stationId}/measurements",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<Measurement> getMeasurements(@PathVariable String stationId) {
        Station s = simulationService.findById(stationId);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Station mit der ID " + stationId + " existiert nicht");
        }
        // Kommentar: Verwende neue Hilfsmethode im Service
        return simulationService.getMeasurements(s);
    }

    @GetMapping(value = "/alerts",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<Station> getAlerts() {
        return simulationService.getStations().stream()
                .filter(s -> s.getWarningLevel() == WarningLevel.WARNING
                        || s.getWarningLevel() == WarningLevel.CRITICAL)
                .toList();
    }
}