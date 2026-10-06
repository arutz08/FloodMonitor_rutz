package com.example.floodmonitor.Repository;

import com.example.floodmonitor.model.Station;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class StationRepository {

    private final List<Station> stations = new CopyOnWriteArrayList<>();

    public List<Station> findAll() {
        return new ArrayList<>(stations);
    }

    public Optional<Station> findById(String id) {
        return stations.stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public Station save(Station station) {
        stations.add(station);
        return station;
    }

    public int count() {
        return stations.size();
    }
}