package com.example.demoaether;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGeneratorTest {

    @Test
    void generatesPdfWithHistory() throws Exception {
        MissionConfig config = new MissionConfig();
        MissionState state = new MissionState(0, 1, 2, 3, 7.8, 6678, 380000, 300);
        CalculationHistoryEntry history = new CalculationHistoryEntry(
                1,
                "Artemis II",
                "Orion",
                0,
                7.8,
                300,
                380000,
                LocalDateTime.now()
        );

        File file = ReportGenerator.generatePdf(config, state, LocalDateTime.now(), List.of(history));

        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }
}
