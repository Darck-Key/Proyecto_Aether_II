package com.example.demoaether;

import org.junit.jupiter.api.Test;
import org.orekit.data.DataContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrekitInitializerTest {

    @Test
    void oam1InitializesOrekitDataContext() {
        assertDoesNotThrow(OrekitInitializer::initialize);
        assertFalse(DataContext.getDefault().getDataProvidersManager().getProviders().isEmpty());
    }
}
