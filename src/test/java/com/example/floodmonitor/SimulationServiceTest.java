package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationServiceTest {

    private StationRepository stationRepository;
    private MeasurementRepository measurementRepository;
    private SimulationService service;

    @BeforeEach
    void setUp() {
        stationRepository = new StationRepository();
        measurementRepository = new MeasurementRepository();
        service = new SimulationService(stationRepository, measurementRepository);
        // @Value greift ohne Spring nicht, daher manuell setzen
        ReflectionTestUtils.setField(service, "intervalMs", 10_000L);
    }

    private Station stationWithMeasurement(double level, double rain, Instant timestamp) {
        Station s = new Station();
        s.setId("1");
        s.setWarningThreshold(3.0);
        s.setCriticalThreshold(4.5);
        stationRepository.save(s);
        measurementRepository.add("1", new Measurement(
                timestamp, level, 10.0, 10.0, rain, 80.0, WarningLevel.NORMAL));
        return s;
    }

    // 8., 9., 10. NORMAL / WARNING / CRITICAL inklusive Grenzwerte
    @ParameterizedTest
    @CsvSource({
            "1.0,  NORMAL",
            "2.99, NORMAL",
            "3.0,  WARNING",
            "4.49, WARNING",
            "4.5,  CRITICAL",
            "6.0,  CRITICAL"
    })
    void warningLevel_dependsOnWaterLevel(double level, WarningLevel expected) {
        Station s = stationWithMeasurement(level, 0.0, Instant.now());
        assertThat(service.calculateWarningLevel(s)).isEqualTo(expected);
    }

    @Test
    void warningLevel_unknownWithoutMeasurement() {
        Station s = new Station();
        s.setId("1");
        stationRepository.save(s);
        assertThat(service.calculateWarningLevel(s)).isEqualTo(WarningLevel.UNKNOWN);
    }

    @Test
    void warningLevel_unknownForOutdatedMeasurement() {
        Station s = stationWithMeasurement(1.0, 0.0, Instant.now().minus(1, ChronoUnit.HOURS));
        assertThat(service.calculateWarningLevel(s)).isEqualTo(WarningLevel.UNKNOWN);
    }

    @Test
    void warningLevel_unknownForDisabledStation() {
        Station s = stationWithMeasurement(5.0, 0.0, Instant.now());
        s.setStatus(StationStatus.DISABLED);
        assertThat(service.calculateWarningLevel(s)).isEqualTo(WarningLevel.UNKNOWN);
    }

    // Simulation ist abschaltbar: keine neuen Messungen
    @Test
    void simulate_doesNothingWhenDisabled() {
        stationWithMeasurement(1.0, 0.0, Instant.now());
        ReflectionTestUtils.setField(service, "enabled", false);

        service.simulate();

        assertThat(measurementRepository.findByStationId("1")).hasSize(1);
    }
}