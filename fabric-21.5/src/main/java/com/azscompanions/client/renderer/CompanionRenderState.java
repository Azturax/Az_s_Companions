package com.azscompanions.client.renderer;

import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.FabricCompanionMode;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Render-state payload for companions (EntityRenderState era). */
public final class CompanionRenderState extends PlayerRenderState {
    public CompanionForm form = CompanionForm.PLAYER;
    public FabricCompanionMode mode = FabricCompanionMode.FOLLOW;
    public boolean slimArms;
    public float bodyScale = 1.0f;
    public float bust = 1.0f;
    public float waist = 1.0f;
    public float hips = 1.0f;
    public float shoulders = 1.0f;
    public float bustOffset = 0.0f;
    public boolean showBust = true;
    public boolean showArmor = true;
    public boolean showNameTag = true;
    public boolean passengerSitPose;
    @Nullable
    public ResourceLocation skinTexture;
    @Nullable
    public ResourceLocation capeTexture;
}
