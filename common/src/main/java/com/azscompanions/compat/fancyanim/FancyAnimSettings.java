package com.azscompanions.compat.fancyanim;

/**
 * Client toggles for Fancy Animations / Fresh Animations / Fresh Moves style packs
 * (OptiFine CEM via EMF + ETF). Soft-compat only — no hard dependency.
 */
public final class FancyAnimSettings {
    public static final String FILE_NAME = "azscompanions-fancyanim.json";

    /**
     * When true <em>and</em> EMF/ETF is present, player-form skins use
     * {@code RenderType.entityTranslucent} for ETF skin transparency / emissives / animated frames.
     * Alone (no packs) this flag does nothing — companions stay on cutout so skins remain visible.
     */
    private boolean translucentPlayerSkins = true;
    /**
     * Mob-form proxy entities reuse the companion UUID so ETF random/emissive variants stay
     * stable and match Fresh Animations CEM on the vanilla proxy renderer.
     */
    private boolean syncMobFormUuid = true;

    public boolean translucentPlayerSkins() {
        return translucentPlayerSkins;
    }

    public void setTranslucentPlayerSkins(boolean translucentPlayerSkins) {
        this.translucentPlayerSkins = translucentPlayerSkins;
    }

    public boolean syncMobFormUuid() {
        return syncMobFormUuid;
    }

    public void setSyncMobFormUuid(boolean syncMobFormUuid) {
        this.syncMobFormUuid = syncMobFormUuid;
    }

    public FancyAnimSettings copy() {
        FancyAnimSettings c = new FancyAnimSettings();
        c.translucentPlayerSkins = translucentPlayerSkins;
        c.syncMobFormUuid = syncMobFormUuid;
        return c;
    }
}
