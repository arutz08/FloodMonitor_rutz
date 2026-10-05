package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Station;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SimulationService {

    private static final String[] STATION_NAMES = {
            "Station Vienna", "Station Linz", "Station Salzburg",
            "Station Graz", "Station Innsbruck"
    };

    private final Random random = new Random();
    private final List<Station> stations = new CopyOnWriteArrayList<>();

    @PostConstruct
    void init() {
        stations.addAll(generateStations());
    }

    public List<Station> getStations() {
        return stations;
    }

    public boolean existsById(String id) {
        return stations.stream().anyMatch(s -> s.getId().equals(id));
    }

    public Station addStation(Station station) {
        stations.add(station);
        return station;
    }

    public int getCount(){
        return stations.size();
    }

    public List<Station> generateStations() {
        List<Station> generated = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        for (int i = 0; i < STATION_NAMES.length; i++) {
            Station station = new Station();
            station.setId(String.valueOf(i + 1));
            station.setStationName(STATION_NAMES[i]);
            station.setTimestamp(timestamp);
            station.setWaterLevel(Math.round((0.5 + random.nextDouble() * 4.5) * 100.0) / 100.0);
            station.setWaterTemperature(Math.round((4.0 + random.nextDouble() * 16.0) * 10.0) / 10.0);
            station.setUnit("m");
            generated.add(station);
        }

        return generated;
    }
}