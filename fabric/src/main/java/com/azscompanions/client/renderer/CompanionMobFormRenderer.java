package com.azscompanions.client.renderer;

import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;

/**
 * Renders non-player companion forms by delegating to vanilla mob renderers.
 * Client-only proxy entities avoid ClassCastException in models that cast to Wolf/Fox/etc.,
 * and LivingEntityRenderer supplies the correct upright orientation in world and GUI.
 * Walk/attack state and equipment are copied each frame so vanilla layers animate and show items.
 */
public final class CompanionMobFormRenderer {
    private static final Field WALK_SPEED_OLD;
    private static final Field WALK_POSITION;

    static {
        try {
            WALK_SPEED_OLD = WalkAnimationState.class.getDeclaredField("speedOld");
            WALK_POSITION = WalkAnimationState.class.getDeclaredField("position");
            WALK_SPEED_OLD.setAccessible(true);
            WALK_POSITION.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private final Map<CompanionForm, LivingEntity> visuals = new EnumMap<>(CompanionForm.class);
    private final ItemInHandRenderer itemInHandRenderer;

    public CompanionMobFormRenderer(EntityRendererProvider.Context context) {
        this.itemInHandRenderer = context.getItemInHandRenderer();
    }

    public void render(FabricCompanionEntity entity, CompanionForm form, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (form == null || form.isPlayer()) {
            return;
        }
        Level level = entity.level();
        if (level == null) {
            return;
        }
        LivingEntity visual = visualFor(form, level);
        if (visual == null) {
            return;
        }

        syncVisual(entity, visual);
        @SuppressWarnings("unchecked")
        EntityRenderer<LivingEntity> renderer =
                (EntityRenderer<LivingEntity>) (EntityRenderer<?>)
                        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(visual);
        renderer.render(visual, entityYaw, partialTicks, poseStack, buffer, packedLight);

        if (!usesHumanoidHeldItems(form)) {
            renderAnimalHeldItems(entity, form, poseStack, buffer, packedLight);
        }
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
        LivingEntity created = type.create(level);
        if (created == null) {
            return null;
        }
        if (created instanceof net.minecraft.world.entity.Mob mob) {
            mob.setNoAi(true);
        }
        created.setSilent(true);
        created.setCustomNameVisible(false);
        created.setPose(Pose.STANDING);
        visuals.put(form, created);
        return created;
    }

    private static void syncVisual(FabricCompanionEntity source, LivingEntity visual) {
        visual.tickCount = source.tickCount;
        visual.setPosRaw(source.getX(), source.getY(), source.getZ());
        visual.xo = source.xo;
        visual.yo = source.yo;
        visual.zo = source.zo;
        visual.setYRot(source.getYRot());
        visual.setXRot(source.getXRot());
        visual.yRotO = source.yRotO;
        visual.xRotO = source.xRotO;
        visual.yBodyRot = source.yBodyRot;
        visual.yBodyRotO = source.yBodyRotO;
        visual.yHeadRot = source.yHeadRot;
        visual.yHeadRotO = source.yHeadRotO;
        copyWalkAnimation(source.walkAnimation, visual.walkAnimation);
        visual.attackAnim = source.attackAnim;
        visual.oAttackAnim = source.oAttackAnim;
        visual.swinging = source.swinging;
        visual.swingingArm = source.swingingArm;
        visual.swingTime = source.swingTime;
        visual.hurtTime = source.hurtTime;
        visual.hurtDuration = source.hurtDuration;
        visual.deathTime = source.deathTime;
        visual.setDeltaMovement(source.getDeltaMovement());
        visual.setPose(source.getPose() == Pose.SLEEPING ? Pose.STANDING : source.getPose());
        visual.setShiftKeyDown(source.isShiftKeyDown());
        visual.setSprinting(source.isSprinting());
        visual.setSwimming(source.isSwimming());
        visual.setCustomNameVisible(false);

        boolean sitting = source.getMode() == FabricCompanionMode.SIT
                || source.getMode() == FabricCompanionMode.STAY;
        boolean aggressive = source.getTarget() != null && source.getTarget().isAlive() || source.swinging;
        if (visual instanceof net.minecraft.world.entity.Mob mob) {
            mob.setAggressive(aggressive);
        }

        var scaleAttr = visual.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(source.getBodyScale());
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = source.getItemBySlot(slot);
            if (!isArmorVisibleForRender(source) && isArmorEquipmentSlot(slot)) {
                stack = ItemStack.EMPTY;
            }
            if (!ItemStack.matches(visual.getItemBySlot(slot), stack)) {
                visual.setItemSlot(slot, stack.copy());
            }
        }

        if (visual instanceof TamableAnimal tamable) {
            tamable.setInSittingPose(sitting);
            tamable.setOrderedToSit(sitting);
        }
        if (visual instanceof Fox fox) {
            fox.setSitting(sitting);
            fox.setIsCrouching(source.isShiftKeyDown());
        }
    }

    private static boolean isArmorVisibleForRender(FabricCompanionEntity source) {
        if (com.azscompanions.client.FabricClientAppearanceDraft.matches(source)) {
            return com.azscompanions.client.FabricClientAppearanceDraft.ACTIVE.showArmor;
        }
        return source.isArmorVisible();
    }

    private static boolean isArmorEquipmentSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET
                || slot == EquipmentSlot.BODY;
    }

    private static void copyWalkAnimation(WalkAnimationState from, WalkAnimationState to) {
        to.setSpeed(from.speed());
        try {
            WALK_SPEED_OLD.setFloat(to, WALK_SPEED_OLD.getFloat(from));
            WALK_POSITION.setFloat(to, WALK_POSITION.getFloat(from));
        } catch (IllegalAccessException ignored) {
        }
    }

    private void renderAnimalHeldItems(FabricCompanionEntity entity, CompanionForm form, PoseStack poseStack,
                                       MultiBufferSource buffer, int packedLight) {
        ItemStack main = entity.getMainHandItem();
        ItemStack off = entity.getOffhandItem();
        if (main.isEmpty() && off.isEmpty()) {
            return;
        }
        float height = form.height() * entity.getBodyScale();
        if (!main.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.28f, height * 0.45f, -0.05f);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
            poseStack.scale(0.7f, 0.7f, 0.7f);
            itemInHandRenderer.renderItem(entity, main, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    false, poseStack, buffer, packedLight);
            poseStack.popPose();
        }
        if (!off.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(-0.28f, height * 0.45f, -0.05f);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
            poseStack.scale(0.7f, 0.7f, 0.7f);
            itemInHandRenderer.renderItem(entity, off, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    true, poseStack, buffer, packedLight);
            poseStack.popPose();
        }
    }

    private static boolean usesHumanoidHeldItems(CompanionForm form) {
        return switch (form) {
            case ZOMBIE, SKELETON, HUSK, STRAY, ENDERMAN -> true;
            default -> false;
        };
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
