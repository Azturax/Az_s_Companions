package com.azscompanions.client.renderer;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Delegates non-player forms to vanilla mob renderers using client-only proxy entities.
 */
public final class CompanionMobFormRenderer {
    private final Map<CompanionForm, LivingEntity> visuals = new EnumMap<>(CompanionForm.class);

    public CompanionMobFormRenderer(EntityRendererProvider.Context context) {
    }

    public void submit(
            CompanionEntity entity,
            CompanionForm form,
            float partialTicks,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (entity == null || form == null || form.isPlayer()) {
            return;
        }
        LivingEntity visual = visualFor(form, entity.level());
        if (visual == null) {
            return;
        }
        syncVisual(entity, form, visual);
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var state = dispatcher.extractEntity(visual, partialTicks);
        dispatcher.submit(state, camera, 0.0d, 0.0d, 0.0d, poseStack, submitNodeCollector);
    }

    private LivingEntity visualFor(CompanionForm form, Level level) {
        LivingEntity existing = visuals.get(form);
        if (existing != null && existing.level() == level) {
            return existing;
        }
        EntityType<? extends LivingEntity> type = vanillaType(form);
        if (type == null) {
            return null;
        }
        LivingEntity created = type.create(level, EntitySpawnReason.LOAD);
        if (created == null) {
            return null;
        }
        if (created instanceof Mob mob) {
            mob.setNoAi(true);
        }
        created.setSilent(true);
        created.setCustomNameVisible(false);
        visuals.put(form, created);
        return created;
    }

    private static void syncVisual(CompanionEntity source, CompanionForm form, LivingEntity visual) {
        if (com.azscompanions.compat.fancyanim.FancyAnimCompat.syncMobFormUuid()) {
            UUID id = source.getUUID();
            if (!id.equals(visual.getUUID())) {
                visual.setUUID(id);
            }
        }
        visual.tickCount = source.tickCount;
        visual.setPosRaw(source.getX(), source.getY(), source.getZ());
        visual.setYRot(source.getYRot());
        visual.setXRot(source.getXRot());
        visual.yRotO = source.yRotO;
        visual.xRotO = source.xRotO;
        visual.yBodyRot = source.yBodyRot;
        visual.yBodyRotO = source.yBodyRotO;
        visual.yHeadRot = source.yHeadRot;
        visual.yHeadRotO = source.yHeadRotO;
        visual.setDeltaMovement(source.getDeltaMovement());
        visual.setPose(source.getPose() == Pose.SLEEPING ? Pose.STANDING : source.getPose());
        visual.setShiftKeyDown(source.isShiftKeyDown());
        visual.setSprinting(source.isSprinting());
        visual.setSwimming(source.isSwimming());
        if (visual instanceof Mob mob) {
            mob.setAggressive(source.getTarget() != null || source.swinging);
        }
        boolean nativeSit = source.getMode() == com.azscompanions.entity.CompanionMode.SIT
                && form.usesNativeAnimalSitPose();
        if (visual instanceof TamableAnimal tamable) {
            tamable.setInSittingPose(nativeSit);
            tamable.setOrderedToSit(nativeSit);
        }
        if (visual instanceof Fox fox) {
            fox.setSitting(nativeSit);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND
                    ? ItemStack.EMPTY
                    : source.getItemBySlot(slot);
            visual.setItemSlot(slot, stack.copy());
        }
    }

    private static EntityType<? extends LivingEntity> vanillaType(CompanionForm form) {
        return switch (form) {
            case CHICKEN -> EntityType.CHICKEN;
            case WOLF -> EntityType.WOLF;
            case CAT -> EntityType.CAT;
            case COW -> EntityType.COW;
            case PIG -> EntityType.PIG;
            case SHEEP -> EntityType.SHEEP;
            case FOX -> EntityType.FOX;
            case RABBIT -> EntityType.RABBIT;
            case BEE -> EntityType.BEE;
            case ZOMBIE -> EntityType.ZOMBIE;
            case SKELETON -> EntityType.SKELETON;
            case SPIDER -> EntityType.SPIDER;
            case ENDERMAN -> EntityType.ENDERMAN;
            case HUSK -> EntityType.HUSK;
            case STRAY -> EntityType.STRAY;
            default -> null;
        };
    }
}
