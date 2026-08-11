package com.azscompanions.entity;

/**
 * Shared Sit-command pose rules (visible pose, not only AI stillness).
 * <ul>
 *   <li>Player + humanoid mob forms → passenger / minecart bent-leg pose</li>
 *   <li>Wolf / cat / fox → native animal sit</li>
 *   <li>Other forms → no dedicated sit mesh (hold still only)</li>
 * </ul>
 */
public final class CompanionSitPose {
    private CompanionSitPose() {
    }

    /** True when Sit should drive a visible sit pose for this form. */
    public static boolean hasVisualSitPose(CompanionForm form) {
        return form != null && (form.usesPassengerSitPose() || form.usesNativeAnimalSitPose());
    }
}
