package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightAuraSupportTest {
    @Test
    void auraOffOnGroundOrFluidOrNimbusRide() {
        assertFalse(FlightAuraSupport.shouldShowAura(true, true, false, false));
        assertFalse(FlightAuraSupport.shouldShowAura(true, false, true, false));
        assertFalse(FlightAuraSupport.shouldShowAura(false, false, false, false));
        assertFalse(FlightAuraSupport.shouldShowAura(true, false, false, true));
        assertTrue(FlightAuraSupport.shouldShowAura(true, false, false, false));
    }

    @Test
    void shellStaysLow() {
        assertTrue(FlightAuraSupport.shellOffsetY(1.8f) <= 1.8f * FlightAuraSupport.MAX_SHELL_HEIGHT_FRACTION + 1.0e-4f);
        assertTrue(FlightAuraSupport.shellOffsetY(1.8f) <= FlightAuraSupport.SHELL_Y_OFFSET + 1.0e-4f);
        assertFalse(FlightAuraSupport.shouldDrawBodyShell(true));
        assertTrue(FlightAuraSupport.shouldDrawBodyShell(false));
    }

    @Test
    void colorResolution() {
        assertEquals(0x112233, FlightAuraSupport.resolveColorRgb(0x112233, null));
        assertEquals(FlightAuraSupport.DEFAULT_KI_RGB, FlightAuraSupport.resolveColorRgb(-1, null));
        assertEquals(0xFF5555, FlightAuraSupport.resolveColorRgb(-1, "red"));
        assertEquals(FlightAuraSupport.DEFAULT_NIMBUS_RGB, FlightAuraSupport.resolveNimbusTrailRgb(null));
    }

    @Test
    void trailBufferNewestFirst() {
        FlightAuraTrailBuffer buf = new FlightAuraTrailBuffer(3);
        buf.push(1, 0, 0);
        buf.push(2, 0, 0);
        buf.push(3, 0, 0);
        double[] out = new double[3];
        assertTrue(buf.getFromNewest(0, out));
        assertEquals(3.0d, out[0], 1.0e-9);
        assertTrue(buf.getFromNewest(2, out));
        assertEquals(1.0d, out[0], 1.0e-9);
        buf.push(4, 0, 0);
        assertTrue(buf.getFromNewest(0, out));
        assertEquals(4.0d, out[0], 1.0e-9);
        assertEquals(3, buf.size());
    }

    @Test
    void nimbusTrailGate() {
        assertFalse(FlightAuraSupport.shouldShowNimbusTrail(false, true));
        assertFalse(FlightAuraSupport.shouldShowNimbusTrail(true, false));
        assertTrue(FlightAuraSupport.shouldShowNimbusTrail(true, true));
        assertTrue(FlightAuraSupport.movingFastEnough(0.2, 0, 0));
        assertFalse(FlightAuraSupport.movingFastEnough(0.01, 0, 0));
    }
}
