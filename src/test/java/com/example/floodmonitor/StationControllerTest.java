package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StationControllerTest {

    private static final String BASE = "/api/v1";
    private static final Instant T0 = Instant.parse("2026-05-01T08:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private StationRepository stationRepository;
    @Autowired private MeasurementRepository measurementRepository;

    /**
     * Feste Testdaten: Station 1 hat 5 Messungen (stündlich 08:00 bis 12:00),
     * Station 2 eine Messung. Der Simulator ist im Profil "test" aus.
     */
    @BeforeEach
    void setUp() {
        stationRepository.deleteAll();
        measurementRepository.deleteAll();

        stationRepository.save(station("1", "Station Vienna", "Donau"));
        stationRepository.save(station("2", "Station Linz", "Donau"));

        double[] levels = {1.0, 2.0, 3.2, 4.8, 1.5};
        WarningLevel[] warn = {WarningLevel.NORMAL, WarningLevel.NORMAL, WarningLevel.WARNING,
                WarningLevel.CRITICAL, WarningLevel.NORMAL};
        for (int i = 0; i < levels.length; i++) {
            measurementRepository.add("1", new Measurement(
                    T0.plus(i, ChronoUnit.HOURS), levels[i], 10.0, levels[i] * 10, 0.0, 80.0, warn[i]));
        }
        measurementRepository.add("2", new Measurement(
                T0, 1.0, 10.0, 10.0, 0.0, 80.0, WarningLevel.NORMAL));
    }

    private Station station(String id, String name, String river) {
        Station s = new Station();
        s.setId(id);
        s.setStationName(name);
        s.setRiver(river);
        return s;
    }

    // 1. Alle Stationen als JSON
    @Test
    void getAllStations_asJson() throws Exception {
        mockMvc.perform(get(BASE + "/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].stationName").value("Station Vienna"));
    }

    // 2. Alle Stationen als XML
    @Test
    void getAllStations_asXml() throws Exception {
        mockMvc.perform(get(BASE + "/stations").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/*/item").nodeCount(2))
                .andExpect(xpath("/*/item[1]/id").string("1"))
                .andExpect(xpath("/*/item[1]/stationName").string("Station Vienna"));
    }

    // 3. Existierende Station
    @Test
    void getExistingStation() throws Exception {
        mockMvc.perform(get(BASE + "/stations/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.river").value("Donau"));
    }

    // 4. Unbekannte Station
    @Test
    void getUnknownStation_returns404() throws Exception {
        mockMvc.perform(get(BASE + "/stations/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // 5. Aktuellster Messwert (12:00 Uhr, Pegel 1.5)
    @Test
    void getLatestMeasurement() throws Exception {
        mockMvc.perform(get(BASE + "/stations/1/measurements/latest").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waterLevel").value(1.5))
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T12:00:00Z"));
    }

    // 6. Filterung nach Zeitraum (09:00 bis 11:00 inklusive = 3 Messungen)
    @Test
    void filterMeasurementsByTimeRange() throws Exception {
        mockMvc.perform(get(BASE + "/stations/1/measurements")
                        .param("from", "2026-05-01T09:00:00Z")
                        .param("to", "2026-05-01T11:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].waterLevel").value(2.0))
                .andExpect(jsonPath("$[2].waterLevel").value(4.8));
    }

    // 7. Ungültiger Zeitraum (from nach to)
    @Test
    void invalidTimeRange_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/stations/1/measurements")
                        .param("from", "2026-05-01T12:00:00Z")
                        .param("to", "2026-05-01T08:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // 7b. Datum nicht parsbar
    @Test
    void invalidDateFormat_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/stations/1/measurements")
                        .param("from", "gestern")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // 11. Nicht unterstütztes Format
    @Test
    void unsupportedFormat_returns406() throws Exception {
        mockMvc.perform(get(BASE + "/stations").accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isNotAcceptable());
    }

    // 12. Maximales Limit
    @Test
    void limitAboveMaximum_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/stations/1/measurements")
                        .param("limit", "1001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void limitAtMaximum_isAccepted() throws Exception {
        mockMvc.perform(get(BASE + "/stations/1/measurements")
                        .param("limit", "1000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    // Zusatz: limit liefert die neuesten N Einträge
    @Test
    void limit_returnsNewestEntries() throws Exception {
        mockMvc.perform(get(BASE + "/stations/1/measurements")
                        .param("limit", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].waterLevel").value(4.8))
                .andExpect(jsonPath("$[1].waterLevel").value(1.5));
    }

    // Zusatz: Filter nach Warnstufe
    @Test
    void filterMeasurementsByWarningLevel() throws Exception {
        mockMvc.perform(get(BASE + "/stations/1/measurements")
                        .param("warningLevel", "CRITICAL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].waterLevel").value(4.8));
    }
}