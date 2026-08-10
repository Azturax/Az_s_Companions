package com.azscompanions.client.renderer;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Renders non-player companion forms using vanilla mob models/textures.
 */
public final class CompanionMobFormRenderer {
    private final EntityModel<LivingEntity> chicken;
    private final EntityModel<LivingEntity> wolf;
    private final EntityModel<LivingEntity> cat;
    private final EntityModel<LivingEntity> cow;
    private final EntityModel<LivingEntity> pig;
    private final EntityModel<LivingEntity> sheep;
    private final EntityModel<LivingEntity> fox;
    private final EntityModel<LivingEntity> rabbit;
    private final EntityModel<LivingEntity> bee;
    private final EntityModel<LivingEntity> zombie;
    private final EntityModel<LivingEntity> skeleton;
    private final EntityModel<LivingEntity> spider;
    private final EntityModel<LivingEntity> enderman;
    private final EntityModel<LivingEntity> husk;
    private final EntityModel<LivingEntity> stray;

    @SuppressWarnings("unchecked")
    public CompanionMobFormRenderer(EntityRendererProvider.Context context) {
        this.chicken = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN));
        this.wolf = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.WolfModel<>(context.bakeLayer(ModelLayers.WOLF));
        this.cat = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.OcelotModel<>(context.bakeLayer(ModelLayers.CAT));
        this.cow = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.CowModel<>(context.bakeLayer(ModelLayers.COW));
        this.pig = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.PigModel<>(context.bakeLayer(ModelLayers.PIG));
        this.sheep = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.SheepModel<>(context.bakeLayer(ModelLayers.SHEEP));
        this.fox = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.FoxModel<>(context.bakeLayer(ModelLayers.FOX));
        this.rabbit = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.RabbitModel<>(context.bakeLayer(ModelLayers.RABBIT));
        this.bee = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.BeeModel<>(context.bakeLayer(ModelLayers.BEE));
        this.zombie = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE));
        this.skeleton = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON));
        this.spider = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER));
        this.enderman = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN));
        this.husk = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.ZombieModel<>(context.bakeLayer(ModelLayers.HUSK));
        this.stray = (EntityModel<LivingEntity>) (EntityModel<?>) new net.minecraft.client.model.SkeletonModel<>(context.bakeLayer(ModelLayers.STRAY));
    }

    public void render(FabricCompanionEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        CompanionForm form = entity.getForm();
        if (form.isPlayer()) {
            return;
        }
        EntityModel<LivingEntity> model = modelFor(form);
        ResourceLocation texture = textureFor(form);
        if (model == null || texture == null) {
            return;
        }

        poseStack.pushPose();
        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        float limbSwing = entity.walkAnimation.position(partialTicks);
        float limbSwingAmount = entity.walkAnimation.speed(partialTicks);
        float ageInTicks = entity.tickCount + partialTicks;
        float netHeadYaw = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot) - bodyYaw;
        float headPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private EntityModel<LivingEntity> modelFor(CompanionForm form) {
        return switch (form) {
            case CHICKEN -> chicken;
            case WOLF -> wolf;
            case CAT -> cat;
            case COW -> cow;
            case PIG -> pig;
            case SHEEP -> sheep;
            case FOX -> fox;
            case RABBIT -> rabbit;
            case BEE -> bee;
            case ZOMBIE -> zombie;
            case SKELETON -> skeleton;
            case SPIDER -> spider;
            case ENDERMAN -> enderman;
            case HUSK -> husk;
            case STRAY -> stray;
            default -> null;
        };
    }

    private static ResourceLocation textureFor(CompanionForm form) {
        return switch (form) {
            case CHICKEN -> ResourceLocation.withDefaultNamespace("textures/entity/chicken.png");
            case WOLF -> ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf.png");
            case CAT -> ResourceLocation.withDefaultNamespace("textures/entity/cat/tabby.png");
            case COW -> ResourceLocation.withDefaultNamespace("textures/entity/cow/cow.png");
            case PIG -> ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");
            case SHEEP -> ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep.png");
            case FOX -> ResourceLocation.withDefaultNamespace("textures/entity/fox/fox.png");
            case RABBIT -> ResourceLocation.withDefaultNamespace("textures/entity/rabbit/brown.png");
            case BEE -> ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");
            case ZOMBIE -> ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
            case SKELETON -> ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
            case SPIDER -> ResourceLocation.withDefaultNamespace("textures/entity/spider/spider.png");
            case ENDERMAN -> ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman.png");
            case HUSK -> ResourceLocation.withDefaultNamespace("textures/entity/zombie/husk.png");
            case STRAY -> ResourceLocation.withDefaultNamespace("textures/entity/skeleton/stray.png");
            default -> null;
        };
    }
}
