package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.service.SimulationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
        List<Station>  stations = simulationService.generateStations();
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

    @GetMapping("/count")
    public int getCount(){
        return simulationService.generateStations().size();
    }
}