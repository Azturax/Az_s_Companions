package com.azscompanions.client.renderer;

import com.azscompanions.client.FabricClientAppearanceDraft;
import com.azscompanions.compat.fancyanim.FancyAnimCompat;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionFormVariants;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renders non-player companion forms by delegating to vanilla mob renderers.
 * Client-only proxy entities avoid ClassCastException in models that cast to Wolf/Fox/etc.,
 * and LivingEntityRenderer supplies the correct upright orientation in world and GUI.
 * Walk/attack state and armor are copied each frame; held items are not rendered in mob form.
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
    /** Client-only mount so humanoid proxies report {@code isPassenger()} for the minecart sit pose. */
    private Entity sitMount;

    public CompanionMobFormRenderer(EntityRendererProvider.Context context) {
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

        syncVisual(entity, form, visual);
        @SuppressWarnings("unchecked")
        EntityRenderer<LivingEntity> renderer =
                (EntityRenderer<LivingEntity>) (EntityRenderer<?>)
                        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(visual);
        renderer.render(visual, entityYaw, partialTicks, poseStack, buffer, packedLight);
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

    private void syncVisual(FabricCompanionEntity source, CompanionForm form, LivingEntity visual) {
        if (FancyAnimCompat.syncMobFormUuid()) {
            UUID id = source.getUUID();
            if (!id.equals(visual.getUUID())) {
                visual.setUUID(id);
            }
        }
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

        // Sit command only — Stay holds still without a sit mesh.
        boolean sitting = source.getMode() == FabricCompanionMode.SIT;
        boolean aggressive = source.getTarget() != null && source.getTarget().isAlive() || source.swinging;
        if (visual instanceof net.minecraft.world.entity.Mob mob) {
            mob.setAggressive(aggressive);
        }

        // no Attributes.SCALE on 1.20.1

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack;
            if (isHandEquipmentSlot(slot)) {
                // Mob forms never show held items (humanoid ItemInHandLayer or animal overlays).
                stack = ItemStack.EMPTY;
            } else if (!isArmorVisibleForRender(source) && isArmorEquipmentSlot(slot)) {
                stack = ItemStack.EMPTY;
            } else {
                stack = source.getItemBySlot(slot);
            }
            if (!ItemStack.matches(visual.getItemBySlot(slot), stack)) {
                visual.setItemSlot(slot, stack.copy());
            }
        }

        boolean nativeSit = sitting && form.usesNativeAnimalSitPose();
        if (visual instanceof TamableAnimal tamable) {
            tamable.setInSittingPose(nativeSit);
            tamable.setOrderedToSit(nativeSit);
        }
        if (visual instanceof Fox fox) {
            fox.setSitting(nativeSit);
            fox.setIsCrouching(source.isShiftKeyDown());
        }
        syncPassengerSitPose(visual, sitting && form.usesPassengerSitPose());
        applyFormVariant(visual, form, resolveFormVariant(source, form));
    }

    private void syncPassengerSitPose(LivingEntity visual, boolean sitting) {
        if (!sitting) {
            if (visual.isPassenger()) {
                visual.stopRiding();
            }
            return;
        }
        Level level = visual.level();
        if (level == null) {
            return;
        }
        Entity mount = sitMountFor(level);
        if (mount == null) {
            return;
        }
        mount.setPosRaw(visual.getX(), visual.getY(), visual.getZ());
        if (visual.getVehicle() != mount) {
            visual.startRiding(mount, true);
        }
    }

    private Entity sitMountFor(Level level) {
        if (sitMount != null && sitMount.level() == level) {
            return sitMount;
        }
        // Invisible client-only mount; humanoid models use isPassenger() for bent-leg sit.
        Entity created = EntityType.MARKER.create(level);
        if (created == null) {
            created = EntityType.MINECART.create(level);
        }
        sitMount = created;
        return sitMount;
    }

    private static String resolveFormVariant(FabricCompanionEntity source, CompanionForm form) {
        if (FabricClientAppearanceDraft.matches(source)
                && FabricClientAppearanceDraft.ACTIVE.formVariant != null) {
            return CompanionFormVariants.normalize(form, FabricClientAppearanceDraft.ACTIVE.formVariant);
        }
        return CompanionFormVariants.normalize(form, source.getFormVariant());
    }

    /** Apply wolf/cat/fox/rabbit/sheep visual variant via entity NBT. */
    private static void applyFormVariant(LivingEntity visual, CompanionForm form, String variantId) {
        if (form == null || !CompanionFormVariants.hasVariants(form)) {
            return;
        }
        String want = CompanionFormVariants.normalize(form, variantId);
        var nbt = new net.minecraft.nbt.CompoundTag();
        visual.saveWithoutId(nbt);
        boolean changed = false;
        switch (form) {
            case WOLF, CAT -> {
                String current = nbt.contains("variant") ? nbt.getString("variant") : "";
                if (!want.equals(current)) {
                    nbt.putString("variant", want);
                    changed = true;
                }
            }
            case FOX -> {
                String current = nbt.contains("Type") ? nbt.getString("Type") : "";
                if (!want.equalsIgnoreCase(current)) {
                    nbt.putString("Type", want);
                    changed = true;
                }
            }
            case RABBIT -> {
                int wantType = CompanionFormVariants.rabbitTypeId(want);
                int current = nbt.contains("RabbitType") ? nbt.getInt("RabbitType") : 0;
                if (wantType != current) {
                    nbt.putInt("RabbitType", wantType);
                    changed = true;
                }
            }
            case SHEEP -> {
                byte wantColor = (byte) CompanionFormVariants.sheepColorId(want);
                byte current = nbt.contains("Color") ? nbt.getByte("Color") : 0;
                if (wantColor != current) {
                    nbt.putByte("Color", wantColor);
                    changed = true;
                }
            }
            default -> {
            }
        }
        if (changed) {
            visual.load(nbt);
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
                /* BODY omitted on 1.20.1 */ || false;
    }

    private static boolean isHandEquipmentSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
    }

    private static void copyWalkAnimation(WalkAnimationState from, WalkAnimationState to) {
        to.setSpeed(from.speed());
        try {
            WALK_SPEED_OLD.setFloat(to, WALK_SPEED_OLD.getFloat(from));
            WALK_POSITION.setFloat(to, WALK_POSITION.getFloat(from));
        } catch (IllegalAccessException ignored) {
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
