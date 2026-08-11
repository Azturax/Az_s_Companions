package com.azscompanions.client;

import com.azscompanions.entity.CompanionBodyProportions;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionGender;
import com.azscompanions.entity.CompanionOrbSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class FabricClientAppearanceDraft {
    public static FabricClientAppearanceDraft ACTIVE;

    public final int entityId;
    public String name = "";
    public float scale = FabricCompanionEntity.DEFAULT_BODY_SCALE;
    public String skinPath = "";
    public String sleepingSkinPath = "";
    public String bathingSkinPath = "";
    public String adventuringSkinPath = "";
    public boolean slimArms;
    public CompanionGender gender = CompanionGender.FEMALE;
    public float bust = CompanionBodyProportions.DEFAULT_BUST;
    public float waist = CompanionBodyProportions.DEFAULT_WAIST;
    public float hips = CompanionBodyProportions.DEFAULT_HIPS;
    public float shoulders = CompanionBodyProportions.DEFAULT_SHOULDERS;
    public float bustOffset = CompanionBodyProportions.DEFAULT_BUST_OFFSET;
    public CompanionForm form = CompanionForm.PLAYER;
    public boolean showNameTag = true;
    public boolean showArmor = true;
    public int orbColorRgb = CompanionOrbSettings.DEFAULT_COLOR_RGB;
    public int orbBrightness = CompanionOrbSettings.DEFAULT_BRIGHTNESS;
    public float orbFloatAmplitude = CompanionOrbSettings.DEFAULT_FLOAT_AMPLITUDE;
    public float orbFloatSpeed = CompanionOrbSettings.DEFAULT_FLOAT_SPEED;
    public float orbFloatHeight = CompanionOrbSettings.DEFAULT_FLOAT_HEIGHT;
    public float orbOffsetX = CompanionOrbSettings.DEFAULT_OFFSET_X;
    public float orbOffsetY = CompanionOrbSettings.DEFAULT_OFFSET_Y;
    public float orbOffsetZ = CompanionOrbSettings.DEFAULT_OFFSET_Z;

    public FabricClientAppearanceDraft(int entityId) {
        this.entityId = entityId;
    }

    public static FabricClientAppearanceDraft from(FabricCompanionEntity entity) {
        FabricClientAppearanceDraft d = new FabricClientAppearanceDraft(entity.getId());
        d.name = entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : entity.getChatDisplayName();
        d.scale = entity.getBodyScale();
        d.skinPath = entity.getSkinPath() == null ? "" : entity.getSkinPath();
        d.sleepingSkinPath = entity.getSleepingSkinPath() == null ? "" : entity.getSleepingSkinPath();
        d.bathingSkinPath = entity.getBathingSkinPath() == null ? "" : entity.getBathingSkinPath();
        d.adventuringSkinPath = entity.getAdventuringSkinPath() == null ? "" : entity.getAdventuringSkinPath();
        d.slimArms = entity.isSlimArms();
        d.gender = entity.getGender();
        d.bust = entity.getBust();
        d.waist = entity.getWaist();
        d.hips = entity.getHips();
        d.shoulders = entity.getShoulders();
        d.bustOffset = entity.getBustOffset();
        d.form = entity.getForm();
        d.showNameTag = entity.isNameTagVisible();
        d.showArmor = entity.isArmorVisible();
        d.orbColorRgb = entity.getOrbColorRgb();
        d.orbBrightness = entity.getOrbBrightness();
        d.orbFloatAmplitude = entity.getOrbFloatAmplitude();
        d.orbFloatSpeed = entity.getOrbFloatSpeed();
        d.orbFloatHeight = entity.getOrbFloatHeight();
        d.orbOffsetX = entity.getOrbOffsetX();
        d.orbOffsetY = entity.getOrbOffsetY();
        d.orbOffsetZ = entity.getOrbOffsetZ();
        return d;
    }

    public static boolean matches(FabricCompanionEntity entity) {
        return ACTIVE != null && ACTIVE.entityId == entity.getId();
    }
}
