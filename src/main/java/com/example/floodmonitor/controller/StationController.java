package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StationController {

    private final SimulationService simulationService;

    public StationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/stations")
    public List<Station> getStations(@RequestParam(required = false) String id) {
        List<Station>  stations = simulationService.getStations();
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

    @PostMapping("/stations")
    public ResponseEntity<Station> addStation(@RequestBody Station station){
        if (simulationService.existsById(station.getId())){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Station mit der ID " + station.getId() + " existiert bereits");
        }
        simulationService.addStation(station);
        return ResponseEntity.status(HttpStatus.CREATED).body(station);
    }

    @GetMapping("/count")
    public int getCount(){
        return simulationService.getCount();
    }
}