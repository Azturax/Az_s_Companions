package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionOrbSupportTest {
    @Test
    void formIsOrbAndSpecialGroup() {
        assertTrue(CompanionForm.GLOWING_ORB.isOrb());
        assertFalse(CompanionForm.PLAYER.isOrb());
        assertEquals(CompanionForm.FormGroup.SPECIAL, CompanionForm.GLOWING_ORB.group());
        assertEquals("glowing_orb", CompanionForm.GLOWING_ORB.serializedName());
        assertEquals("Glowing Orb", CompanionForm.GLOWING_ORB.displayLabel());
        assertEquals(CompanionForm.GLOWING_ORB, CompanionForm.byName("glowing_orb"));
        assertEquals(CompanionForm.GLOWING_ORB, CompanionForm.byName("orb"));
        assertFalse(CompanionForm.GLOWING_ORB.supportsHumanoidArmor());
        assertFalse(CompanionForm.GLOWING_ORB.supportsWolfArmor());
    }

    @Test
    void clampsColorBrightnessFloatAndOffsets() {
        assertEquals(CompanionOrbSupport.DEFAULT_COLOR_RGB, 0xFFF5E6);
        assertEquals(14, CompanionOrbSupport.DEFAULT_BRIGHTNESS);
        assertEquals(CompanionOrbSupport.TORCH_BRIGHTNESS, CompanionOrbSupport.DEFAULT_BRIGHTNESS);
        assertFalse(CompanionOrbSupport.DEFAULT_FRONT);
        assertEquals(0x00FF00, CompanionOrbSupport.clampRgb(0xFF00FF00));
        assertEquals(0x112233, CompanionOrbSupport.rgb(0x11, 0x22, 0x33));
        assertEquals(15, CompanionOrbSupport.clampBrightness(99));
        assertEquals(0, CompanionOrbSupport.clampBrightness(-3));
        assertEquals(14, CompanionOrbSupport.lightLuminance(true, 14));
        assertEquals(0, CompanionOrbSupport.lightLuminance(false, 15));
        assertEquals(0.75f, CompanionOrbSupport.clampFloatAmplitude(2.0f), 1.0e-4f);
        assertEquals(0.0f, CompanionOrbSupport.clampFloatAmplitude(-1.0f), 1.0e-4f);
        assertEquals(8.0f, CompanionOrbSupport.clampOffset(99.0f), 1.0e-4f);
        assertEquals(-8.0f, CompanionOrbSupport.clampOffset(-99.0f), 1.0e-4f);
    }

    @Test
    void particleCountsScaleWithBrightness() {
        assertEquals(0, CompanionOrbSupport.dustParticlesPerTick(0, 1.0f));
        assertTrue(CompanionOrbSupport.dustParticlesPerTick(14, 1.0f) >= 8);
        assertTrue(CompanionOrbSupport.glowParticlesPerTick(14) >= 1);
        assertEquals(0, CompanionOrbSupport.glowParticlesPerTick(3));
        float[] out = new float[3];
        CompanionOrbSupport.sampleBallOffset(1, 2, 0.5f, out);
        float len = (float) Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]);
        assertTrue(len <= 0.5f + 1.0e-4f);
    }

    @Test
    void bobAndOffsets() {
        float bob = CompanionOrbSupport.bobDeltaY(10, 0.0f, 0.2f, 0.5f);
        assertTrue(Math.abs(bob) <= 0.2f + 1.0e-4f);
        double[] world = CompanionOrbSupport.worldOffsetFromLocal(0.0f, 1.0f, 0.5f, 0.0f);
        assertEquals(1.0d, world[0], 1.0e-6d);
        assertEquals(0.5d, world[1], 1.0e-6d);
        assertEquals(0.0d, world[2], 1.0e-6d);
    }

    @Test
    void preferredTargetFrontAndBack() {
        double[] back = CompanionOrbFlightSupport.preferredTarget(
                0, 64, 0, 0.0f, 3, 0, 2.0d, 1.5f, 0, 0, 0, false);
        double[] front = CompanionOrbFlightSupport.preferredTarget(
                0, 64, 0, 0.0f, 3, 0, 2.0d, 1.5f, 0, 0, 0, true);
        assertEquals(64.0d + 1.5d, back[1], 1.0e-4d);
        assertEquals(64.0d + 1.5d, front[1], 1.0e-4d);
        // yaw 0 → +Z forward; back is −Z, front is +Z
        assertTrue(back[2] < 0.0d);
        assertTrue(front[2] > 0.0d);
        assertEquals(-back[2], front[2], 1.0e-4d);
    }

    @Test
    void reflectiveLuminanceWithoutMinecraftTypes() {
        Object fake = new Object() {
            public CompanionForm getForm() {
                return CompanionForm.GLOWING_ORB;
            }

            public int getOrbBrightness() {
                return 11;
            }
        };
        assertEquals(11, CompanionOrbSupport.lightLuminanceReflective(fake));
        assertEquals(0, CompanionOrbSupport.lightLuminanceReflective(new Object()));
    }
}
