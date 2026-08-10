package com.azscompanions.client;

import com.azscompanions.entity.CompanionBodyProportions;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionGender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class FabricClientAppearanceDraft {
    public static FabricClientAppearanceDraft ACTIVE;

    public final int entityId;
    public String name = "";
    public float scale = FabricCompanionEntity.DEFAULT_BODY_SCALE;
    public String skinPath = "";
    public boolean slimArms;
    public CompanionGender gender = CompanionGender.FEMALE;
    public float bust = CompanionBodyProportions.DEFAULT_BUST;
    public float waist = CompanionBodyProportions.DEFAULT_WAIST;
    public float hips = CompanionBodyProportions.DEFAULT_HIPS;
    public float shoulders = CompanionBodyProportions.DEFAULT_SHOULDERS;
    public float bustOffset = CompanionBodyProportions.DEFAULT_BUST_OFFSET;
    public CompanionForm form = CompanionForm.PLAYER;
    public boolean showNameTag = true;

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
        d.slimArms = entity.isSlimArms();
        d.gender = entity.getGender();
        d.bust = entity.getBust();
        d.waist = entity.getWaist();
        d.hips = entity.getHips();
        d.shoulders = entity.getShoulders();
        d.bustOffset = entity.getBustOffset();
        d.form = entity.getForm();
        d.showNameTag = entity.isNameTagVisible();
        return d;
    }

    public static boolean matches(FabricCompanionEntity entity) {
        return ACTIVE != null && ACTIVE.entityId == entity.getId();
    }
}
