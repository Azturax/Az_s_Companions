package com.azscompanions.client.renderer;

import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionMode;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/** Render-state payload for companions (AvatarRenderState / NeoForge 26.2). */
public final class CompanionRenderState extends AvatarRenderState {
    @Nullable
    public com.azscompanions.entity.CompanionEntity source;
    public CompanionForm form = CompanionForm.PLAYER;
    public CompanionMode mode = CompanionMode.FOLLOW;
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
    public Identifier skinTexture;
    @Nullable
    public Identifier capeTexture;
}