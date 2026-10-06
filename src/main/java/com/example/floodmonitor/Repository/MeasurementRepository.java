package com.example.floodmonitor.Repository;

import com.example.floodmonitor.model.Measurement;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class MeasurementRepository {

    private static final int MAX_HISTORY = 1000;

    private final Map<String, List<Measurement>> data = new ConcurrentHashMap<>();

    public void add(String stationId, Measurement m) {
        List<Measurement> list = data.computeIfAbsent(stationId, k -> new CopyOnWriteArrayList<>());
        list.add(m);
        while (list.size() > MAX_HISTORY) {
            list.remove(0);
        }
    }

    public List<Measurement> findByStationId(String stationId) {
        return new ArrayList<>(data.getOrDefault(stationId, List.of()));
    }

    public Optional<Measurement> findLatest(String stationId) {
        List<Measurement> list = data.get(stationId);
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(list.size() - 1));
    }
}