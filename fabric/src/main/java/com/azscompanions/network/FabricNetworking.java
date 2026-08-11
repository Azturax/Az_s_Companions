package com.azscompanions.network;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.ai.CompanionStatsText;
import com.azscompanions.entity.CompanionContextSkinSupport;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionGender;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.entity.FabricCompanionRecruitment;
import com.azscompanions.item.FabricCharmData;
import com.azscompanions.item.FabricCompanionCharmItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class FabricNetworking {
    private FabricNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(RecruitPayload.TYPE, RecruitPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SettingsPayload.TYPE, SettingsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ContextSkinsPayload.TYPE, ContextSkinsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OrbSettingsPayload.TYPE, OrbSettingsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BehaviorPayload.TYPE, BehaviorPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MenuActionPayload.TYPE, MenuActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PersonaPayload.TYPE, PersonaPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminAiSavePayload.TYPE, AdminAiSavePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminActionPayload.TYPE, AdminActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenMenuPayload.TYPE, OpenMenuPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenPersonaPayload.TYPE, OpenPersonaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenStatsPayload.TYPE, OpenStatsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenAdminPayload.TYPE, OpenAdminPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TeamFightHudPayload.TYPE, TeamFightHudPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AiThinkingPayload.TYPE, AiThinkingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DepositSelectionPayload.TYPE, DepositSelectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DepositExitPayload.TYPE, DepositExitPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AiJoinOfferPayload.TYPE, AiJoinOfferPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AiJoinConsentPayload.TYPE, AiJoinConsentPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleWigglyDogPayload.TYPE, ToggleWigglyDogPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RecruitPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    FabricCompanionEntity created = FabricCompanionRecruitment.recruitEntity(
                            context.player(), payload.definitionId());
                    if (created != null) {
                        com.azscompanions.ai.FabricCompanionPersonaOnboarding.offerIfNeeded(context.player(), created);
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(MenuActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (!(entity instanceof FabricCompanionEntity companion)
                            || (!companion.isOwnedBy(player) && !companion.isTrusted(player))) {
                        if (entity instanceof FabricCompanionEntity) {
                            player.displayClientMessage(
                                    Component.translatable("message.azscompanions.not_owner"),
                                    true);
                        }
                        return;
                    }
                    if (companion.distanceTo(player) > 64.0d) {
                        return;
                    }
                    switch (payload.action()) {
                        case "OPEN_INVENTORY" -> companion.openInventory(player);
                        case "OPEN_STATS" -> openStats(player, companion);
                        case "FOLLOW" -> {
                            companion.setMode(FabricCompanionMode.FOLLOW);
                            toastMode(player, companion, "message.azscompanions.mode_follow");
                        }
                        case "STAY" -> {
                            companion.setMode(FabricCompanionMode.STAY);
                            toastMode(player, companion, "message.azscompanions.mode_stay");
                        }
                        case "SIT" -> {
                            companion.setMode(FabricCompanionMode.SIT);
                            toastMode(player, companion, "message.azscompanions.mode_sit");
                        }
                        case "WANDER" -> {
                            companion.setMode(FabricCompanionMode.WANDER);
                            toastMode(player, companion, "message.azscompanions.mode_wander");
                        }
                        case "REMOVE_CHILD" -> {
                            if (companion.isChildCompanion()) {
                                FabricCompanionEntity parent =
                                        FabricCompanionRecruitment.resolveLeader(player, companion);
                                if (parent != null && parent.storeChild(companion)) {
                                    player.displayClientMessage(Component.translatable(
                                            "message.azscompanions.child_stored"), true);
                                }
                            } else if (companion.storeNextLivingChild()) {
                                player.displayClientMessage(Component.translatable(
                                        "message.azscompanions.child_stored"), true);
                            } else {
                                player.displayClientMessage(Component.translatable(
                                        "message.azscompanions.child_none_to_store"), true);
                            }
                        }
                        case "CALL_STORED_CHILD" -> {
                            if (!companion.isChildCompanion()) {
                                FabricCompanionEntity called = companion.callNextStoredChild(player);
                                if (called != null) {
                                    player.displayClientMessage(Component.translatable(
                                            "message.azscompanions.child_called",
                                            called.getChatDisplayName()), true);
                                } else if (companion.getStoredChildCount() <= 0) {
                                    player.displayClientMessage(Component.translatable(
                                            "message.azscompanions.child_none_stored"), true);
                                } else {
                                    player.displayClientMessage(Component.translatable(
                                            "message.azscompanions.child_limit_reached"), true);
                                }
                            }
                        }
                        case "DEPOSIT_SELECT" -> {
                            player.closeContainer();
                            com.azscompanions.deposit.FabricDepositCommands.enable(player);
                        }
                        case "DEPOSIT_DONE" -> com.azscompanions.deposit.FabricDepositCommands.done(player);
                        case "DEPOSIT_CLEAR" -> com.azscompanions.deposit.FabricDepositCommands.clear(player);
                        default -> {
                        }
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (!(entity instanceof FabricCompanionEntity companion) || !companion.isOwnedBy(player)) {
                        return;
                    }
                    if (companion.distanceTo(player) > 16.0d) {
                        return;
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_NAME) != 0 && !payload.name().isBlank()) {
                        String name = payload.name().trim();
                        if (name.length() > 32) {
                            name = name.substring(0, 32);
                        }
                        companion.setCustomDisplayName(name);
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_SCALE) != 0) {
                        companion.setBodyScale(payload.scale());
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_SKIN) != 0) {
                        String skin = payload.skinPath().trim();
                        if (!skin.startsWith("http:") && !skin.startsWith("https:")) {
                            companion.setSkinPath(skin);
                        }
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_SLIM) != 0) {
                        companion.setSlimArms(payload.slimArms());
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_GENDER) != 0) {
                        companion.setGender(payload.male() ? CompanionGender.MALE : CompanionGender.FEMALE);
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_PROPORTIONS) != 0) {
                        companion.setBust(payload.bust());
                        companion.setWaist(payload.waist());
                        companion.setHips(payload.hips());
                        companion.setShoulders(payload.shoulders());
                        companion.setBustOffset(payload.bustOffset());
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_FORM) != 0) {
                        companion.setForm(CompanionForm.byName(payload.form()));
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_SHOW_NAME) != 0) {
                        companion.setNameTagVisible(payload.showNameTag());
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_SHOW_ARMOR) != 0) {
                        companion.setArmorVisible(payload.showArmor());
                    }
                    // Creator Done sends all appearance flags — offer onboarding once if still unset.
                    if ((payload.flags() & SettingsPayload.FLAG_FORM) != 0
                            && (payload.flags() & SettingsPayload.FLAG_NAME) != 0) {
                        com.azscompanions.ai.FabricCompanionPersonaOnboarding.offerIfNeeded(player, companion);
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(ContextSkinsPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (!(entity instanceof FabricCompanionEntity companion)
                            || (!companion.isOwnedBy(player) && !companion.isTrusted(player))) {
                        return;
                    }
                    if (companion.distanceTo(player) > 16.0d) {
                        return;
                    }
                    companion.setContextSkins(
                            payload.sleepingSkin(), payload.bathingSkin(), payload.adventuringSkin());
                }));

        ServerPlayNetworking.registerGlobalReceiver(OrbSettingsPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (!(entity instanceof FabricCompanionEntity companion)
                            || (!companion.isOwnedBy(player) && !companion.isTrusted(player))) {
                        return;
                    }
                    if (companion.distanceTo(player) > 16.0d) {
                        return;
                    }
                    companion.setOrbSettings(
                            payload.colorRgb(),
                            payload.brightness(),
                            payload.floatAmplitude(),
                            payload.floatSpeed(),
                            payload.floatHeight(),
                            payload.offsetX(),
                            payload.offsetY(),
                            payload.offsetZ(),
                            payload.front());
                }));

        ServerPlayNetworking.registerGlobalReceiver(BehaviorPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (!(entity instanceof FabricCompanionEntity companion) || !companion.isOwnedBy(player)) {
                        return;
                    }
                    if (companion.distanceTo(player) > 16.0d) {
                        return;
                    }
                    companion.setFollowRadius(CompanionFollowDistances.clampFollowRadius(payload.followRadius()));
                    companion.setPersonalSpace(CompanionFollowDistances.clampPersonalSpace(payload.personalSpace()));
                    companion.setWanderRadius(CompanionFollowDistances.clampWanderRadius(payload.wanderRadius()));
                }));

        ServerPlayNetworking.registerGlobalReceiver(PersonaPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (!(entity instanceof FabricCompanionEntity companion) || !companion.isOwnedBy(player)) {
                        return;
                    }
                    var current = companion.getPersona();
                    var next = payload.skip()
                            ? current.cleared()
                            : new com.azscompanions.ai.CompanionPersona(
                                    payload.whoAmI(),
                                    payload.whatAmIDoing(),
                                    payload.howWillIBe(),
                                    payload.speechStyle(),
                                    payload.relationshipToOwner(),
                                    payload.quirks(),
                                    true);
                    companion.setPersona(next);
                    player.displayClientMessage(Component.literal(
                            companion.getChatDisplayName() + " — persona "
                                    + (payload.skip() ? "skipped (defaults)" : "saved")), true);
                }));

        ServerPlayNetworking.registerGlobalReceiver(AdminAiSavePayload.TYPE, (payload, context) ->
                context.server().execute(() -> com.azscompanions.admin.FabricAzAdminActions.saveAiConfig(
                        context.player(),
                        com.azscompanions.admin.AdminAiConfigSnapshot.fromWireJson(payload.json()))));

        ServerPlayNetworking.registerGlobalReceiver(AdminActionPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        com.azscompanions.admin.FabricAzAdminActions.handleAction(context.player(), payload.action())));

        ServerPlayNetworking.registerGlobalReceiver(DepositExitPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    com.azscompanions.deposit.DepositChestSelection sel =
                            com.azscompanions.deposit.DepositChestSelection.of(player.getUUID());
                    if (!sel.isSelecting()) {
                        return;
                    }
                    sel.finishKeepingSelection();
                    com.azscompanions.deposit.FabricDepositCommands.sync(player, sel);
                    player.displayClientMessage(Component.translatable(
                            "message.azscompanions.deposit_done", sel.size()), true);
                }));

        ServerPlayNetworking.registerGlobalReceiver(AiJoinConsentPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        com.azscompanions.ai.FabricAiJoinOfferEvents.handleConsent(
                                context.player(),
                                payload.accepted(),
                                payload.suggestProfile(),
                                payload.applyProfile())));

        ServerPlayNetworking.registerGlobalReceiver(ToggleWigglyDogPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (!com.azscompanions.perk.WigglyDogPerkSupport.isEligible(player.getUUID())) {
                        player.displayClientMessage(
                                Component.translatable("message.azscompanions.wiggly_dog_denied"), true);
                        return;
                    }
                    com.azscompanions.perk.WigglyDogPerk.toggle(player);
                }));
    }

    public static void openMenu(ServerPlayer player, FabricCompanionEntity companion) {
        ServerPlayNetworking.send(player, new OpenMenuPayload(companion.getId()));
    }

    public static void openPersonaSetup(ServerPlayer player, FabricCompanionEntity companion) {
        var p = companion.getPersona();
        ServerPlayNetworking.send(player, new OpenPersonaPayload(
                companion.getId(),
                p.whoAmI(),
                p.whatAmIDoing(),
                p.howWillIBe(),
                p.speechStyle(),
                p.relationshipToOwner(),
                p.quirks()));
    }

    public static void openStats(ServerPlayer player, FabricCompanionEntity companion) {
        var p = companion.getPersona();
        ServerPlayNetworking.send(player, new OpenStatsPayload(
                companion.getId(),
                CompanionStatsText.personaSnippet(p.whoAmI()),
                CompanionStatsText.personaSnippet(p.whatAmIDoing()),
                CompanionStatsText.personaSnippet(p.howWillIBe()),
                FabricCompanionRecruitment.countChildrenOf(player, companion.getUUID()),
                (int) FabricCompanionRecruitment.countOwned(player),
                resolveCharmStatus(player),
                CompanionStatsText.aiStatusIfEnabled()));
    }

    /** unbound | bound_active | bound_stored | none */
    public static String resolveCharmStatus(ServerPlayer player) {
        boolean anyCharm = false;
        boolean bound = false;
        boolean stored = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof FabricCompanionCharmItem)) {
                continue;
            }
            anyCharm = true;
            if (FabricCharmData.isBound(stack)) {
                bound = true;
                if (FabricCharmData.hasStoredCompanion(stack)) {
                    stored = true;
                }
            }
        }
        if (!anyCharm) {
            return "none";
        }
        if (stored) {
            return "bound_stored";
        }
        if (bound) {
            return "bound_active";
        }
        return "unbound";
    }

    public static void openAdminPanel(ServerPlayer player,
                                      com.azscompanions.admin.AdminAiConfigSnapshot snap,
                                      String aiStatus,
                                      boolean chunkLoading,
                                      boolean teamfight,
                                      String companionSummary) {
        String json = snap == null ? "{}" : snap.toWireJson();
        ServerPlayNetworking.send(player, new OpenAdminPayload(
                json,
                aiStatus == null ? "" : aiStatus,
                chunkLoading,
                teamfight,
                companionSummary == null ? "" : companionSummary));
    }

    public static void sendTeamFightHud(ServerPlayer player, String encodedSnapshot) {
        ServerPlayNetworking.send(player, new TeamFightHudPayload(encodedSnapshot == null ? "" : encodedSnapshot));
    }

    public static void sendAiThinking(ServerPlayer player, boolean active, String companionName,
                                      int timeoutSeconds, float progress) {
        ServerPlayNetworking.send(player, new AiThinkingPayload(
                active,
                companionName == null ? "" : companionName,
                timeoutSeconds,
                progress));
    }

    public static void sendAiJoinOffer(ServerPlayer player, com.azscompanions.ai.AiJoinOffer offer) {
        com.azscompanions.ai.AiJoinOffer o = offer == null
                ? com.azscompanions.ai.AiJoinOffer.none()
                : offer;
        ServerPlayNetworking.send(player, new AiJoinOfferPayload(
                o.available(),
                o.source(),
                o.providerLabel(),
                o.endpointHint(),
                o.suggestProfile(),
                o.allowApply(),
                o.allowLocalProbe()));
    }

    private static void toastMode(ServerPlayer player, FabricCompanionEntity companion, String key) {
        player.displayClientMessage(
                Component.literal(companion.getChatDisplayName() + " — ")
                        .append(Component.translatable(key)),
                true);
    }

    public record OpenMenuPayload(int entityId) implements CustomPacketPayload {
        public static final Type<OpenMenuPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "open_menu"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenMenuPayload::entityId, OpenMenuPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenPersonaPayload(
            int entityId,
            String whoAmI,
            String whatAmIDoing,
            String howWillIBe,
            String speechStyle,
            String relationshipToOwner,
            String quirks
    ) implements CustomPacketPayload {
        public static final Type<OpenPersonaPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "open_persona"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPersonaPayload> CODEC =
                StreamCodec.of(OpenPersonaPayload::write, OpenPersonaPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, OpenPersonaPayload p) {
            buf.writeVarInt(p.entityId);
            int max = com.azscompanions.ai.CompanionPersona.MAX_LEN;
            buf.writeUtf(p.whoAmI == null ? "" : p.whoAmI, max);
            buf.writeUtf(p.whatAmIDoing == null ? "" : p.whatAmIDoing, max);
            buf.writeUtf(p.howWillIBe == null ? "" : p.howWillIBe, max);
            buf.writeUtf(p.speechStyle == null ? "" : p.speechStyle, max);
            buf.writeUtf(p.relationshipToOwner == null ? "" : p.relationshipToOwner, max);
            buf.writeUtf(p.quirks == null ? "" : p.quirks, max);
        }

        private static OpenPersonaPayload read(RegistryFriendlyByteBuf buf) {
            int max = com.azscompanions.ai.CompanionPersona.MAX_LEN;
            return new OpenPersonaPayload(
                    buf.readVarInt(),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readUtf(max));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenStatsPayload(
            int entityId,
            String whoAmI,
            String whatAmIDoing,
            String howWillIBe,
            int childCount,
            int ownedCount,
            String charmStatus,
            String aiStatus
    ) implements CustomPacketPayload {
        public static final Type<OpenStatsPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "open_stats"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenStatsPayload> CODEC =
                StreamCodec.of(OpenStatsPayload::write, OpenStatsPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, OpenStatsPayload p) {
            buf.writeVarInt(p.entityId);
            int max = CompanionPersona.MAX_LEN;
            buf.writeUtf(p.whoAmI == null ? "" : p.whoAmI, max);
            buf.writeUtf(p.whatAmIDoing == null ? "" : p.whatAmIDoing, max);
            buf.writeUtf(p.howWillIBe == null ? "" : p.howWillIBe, max);
            buf.writeVarInt(p.childCount);
            buf.writeVarInt(p.ownedCount);
            buf.writeUtf(p.charmStatus == null ? "none" : p.charmStatus, 32);
            buf.writeUtf(p.aiStatus == null ? "" : p.aiStatus, CompanionStatsText.AI_SNIPPET + 8);
        }

        private static OpenStatsPayload read(RegistryFriendlyByteBuf buf) {
            int max = CompanionPersona.MAX_LEN;
            return new OpenStatsPayload(
                    buf.readVarInt(),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readUtf(32),
                    buf.readUtf(CompanionStatsText.AI_SNIPPET + 8));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenAdminPayload(
            String aiJson,
            String aiStatus,
            boolean chunkLoading,
            boolean teamfight,
            String companionSummary
    ) implements CustomPacketPayload {
        public static final Type<OpenAdminPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "open_admin"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenAdminPayload> CODEC =
                StreamCodec.of(OpenAdminPayload::write, OpenAdminPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, OpenAdminPayload p) {
            buf.writeUtf(p.aiJson == null ? "{}" : p.aiJson, com.azscompanions.admin.AdminAiConfigSnapshot.MAX_WIRE_JSON);
            buf.writeUtf(p.aiStatus == null ? "" : p.aiStatus, 512);
            buf.writeBoolean(p.chunkLoading);
            buf.writeBoolean(p.teamfight);
            buf.writeUtf(p.companionSummary == null ? "" : p.companionSummary, 1024);
        }

        private static OpenAdminPayload read(RegistryFriendlyByteBuf buf) {
            return new OpenAdminPayload(
                    buf.readUtf(com.azscompanions.admin.AdminAiConfigSnapshot.MAX_WIRE_JSON),
                    buf.readUtf(512),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readUtf(1024));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AdminAiSavePayload(String json) implements CustomPacketPayload {
        public static final Type<AdminAiSavePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "admin_ai_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AdminAiSavePayload> CODEC =
                StreamCodec.of(AdminAiSavePayload::write, AdminAiSavePayload::read);

        private static void write(RegistryFriendlyByteBuf buf, AdminAiSavePayload p) {
            buf.writeUtf(p.json == null ? "{}" : p.json, com.azscompanions.admin.AdminAiConfigSnapshot.MAX_WIRE_JSON);
        }

        private static AdminAiSavePayload read(RegistryFriendlyByteBuf buf) {
            return new AdminAiSavePayload(buf.readUtf(com.azscompanions.admin.AdminAiConfigSnapshot.MAX_WIRE_JSON));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AdminActionPayload(String action) implements CustomPacketPayload {
        public static final Type<AdminActionPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "admin_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AdminActionPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, AdminActionPayload::action,
                        AdminActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PersonaPayload(
            int entityId,
            String whoAmI,
            String whatAmIDoing,
            String howWillIBe,
            String speechStyle,
            String relationshipToOwner,
            String quirks,
            boolean skip
    ) implements CustomPacketPayload {
        public static final Type<PersonaPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "companion_persona"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PersonaPayload> CODEC =
                StreamCodec.of(PersonaPayload::write, PersonaPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, PersonaPayload p) {
            buf.writeVarInt(p.entityId);
            int max = com.azscompanions.ai.CompanionPersona.MAX_LEN;
            buf.writeUtf(p.whoAmI == null ? "" : p.whoAmI, max);
            buf.writeUtf(p.whatAmIDoing == null ? "" : p.whatAmIDoing, max);
            buf.writeUtf(p.howWillIBe == null ? "" : p.howWillIBe, max);
            buf.writeUtf(p.speechStyle == null ? "" : p.speechStyle, max);
            buf.writeUtf(p.relationshipToOwner == null ? "" : p.relationshipToOwner, max);
            buf.writeUtf(p.quirks == null ? "" : p.quirks, max);
            buf.writeBoolean(p.skip);
        }

        private static PersonaPayload read(RegistryFriendlyByteBuf buf) {
            int max = com.azscompanions.ai.CompanionPersona.MAX_LEN;
            return new PersonaPayload(
                    buf.readVarInt(),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readUtf(max),
                    buf.readBoolean());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TeamFightHudPayload(String payload) implements CustomPacketPayload {
        public static final Type<TeamFightHudPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "teamfight_hud"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TeamFightHudPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, TeamFightHudPayload::payload, TeamFightHudPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AiThinkingPayload(
            boolean active,
            String companionName,
            int timeoutSeconds,
            float progress
    ) implements CustomPacketPayload {
        public static final Type<AiThinkingPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "companion_ai_thinking"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AiThinkingPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, AiThinkingPayload::active,
                        ByteBufCodecs.STRING_UTF8, AiThinkingPayload::companionName,
                        ByteBufCodecs.VAR_INT, AiThinkingPayload::timeoutSeconds,
                        ByteBufCodecs.FLOAT, AiThinkingPayload::progress,
                        AiThinkingPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AiJoinOfferPayload(
            boolean available,
            String source,
            String providerLabel,
            String endpointHint,
            String suggestProfile,
            boolean allowApply,
            boolean allowLocalProbe
    ) implements CustomPacketPayload {
        public static final Type<AiJoinOfferPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "ai_join_offer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AiJoinOfferPayload> CODEC =
                StreamCodec.of(AiJoinOfferPayload::write, AiJoinOfferPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, AiJoinOfferPayload p) {
            buf.writeBoolean(p.available);
            buf.writeUtf(p.source == null ? "" : p.source, 16);
            buf.writeUtf(p.providerLabel == null ? "" : p.providerLabel, 64);
            buf.writeUtf(p.endpointHint == null ? "" : p.endpointHint, 128);
            buf.writeUtf(p.suggestProfile == null ? "" : p.suggestProfile, 32);
            buf.writeBoolean(p.allowApply);
            buf.writeBoolean(p.allowLocalProbe);
        }

        private static AiJoinOfferPayload read(RegistryFriendlyByteBuf buf) {
            return new AiJoinOfferPayload(
                    buf.readBoolean(),
                    buf.readUtf(16),
                    buf.readUtf(64),
                    buf.readUtf(128),
                    buf.readUtf(32),
                    buf.readBoolean(),
                    buf.readBoolean());
        }

        public com.azscompanions.ai.AiJoinOffer toOffer() {
            return new com.azscompanions.ai.AiJoinOffer(
                    available, source, providerLabel, endpointHint, suggestProfile, allowApply, allowLocalProbe);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AiJoinConsentPayload(
            boolean accepted,
            String suggestProfile,
            boolean applyProfile
    ) implements CustomPacketPayload {
        public static final Type<AiJoinConsentPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "ai_join_consent"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AiJoinConsentPayload> CODEC =
                StreamCodec.of(AiJoinConsentPayload::write, AiJoinConsentPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, AiJoinConsentPayload p) {
            buf.writeBoolean(p.accepted);
            buf.writeUtf(p.suggestProfile == null ? "" : p.suggestProfile, 32);
            buf.writeBoolean(p.applyProfile);
        }

        private static AiJoinConsentPayload read(RegistryFriendlyByteBuf buf) {
            return new AiJoinConsentPayload(buf.readBoolean(), buf.readUtf(32), buf.readBoolean());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DepositSelectionPayload(String payload) implements CustomPacketPayload {
        public static final Type<DepositSelectionPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "deposit_selection"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DepositSelectionPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, DepositSelectionPayload::payload, DepositSelectionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DepositExitPayload() implements CustomPacketPayload {
        public static final Type<DepositExitPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "deposit_exit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DepositExitPayload> CODEC =
                StreamCodec.unit(new DepositExitPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ToggleWigglyDogPayload() implements CustomPacketPayload {
        public static final Type<ToggleWigglyDogPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "toggle_wiggly_dog"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleWigglyDogPayload> CODEC =
                StreamCodec.unit(new ToggleWigglyDogPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MenuActionPayload(int entityId, String action) implements CustomPacketPayload {
        public static final Type<MenuActionPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "menu_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, MenuActionPayload::entityId,
                        ByteBufCodecs.STRING_UTF8, MenuActionPayload::action,
                        MenuActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RecruitPayload(String definitionId) implements CustomPacketPayload {
        public static final Type<RecruitPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "recruit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RecruitPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RecruitPayload::definitionId, RecruitPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SettingsPayload(
            int entityId,
            String name,
            float scale,
            String skinPath,
            boolean slimArms,
            boolean male,
            float bust,
            float waist,
            float hips,
            float shoulders,
            float bustOffset,
            String form,
            boolean showNameTag,
            boolean showArmor,
            int flags
    ) implements CustomPacketPayload {
        public static final int FLAG_NAME = 1;
        public static final int FLAG_SCALE = 2;
        public static final int FLAG_SKIN = 4;
        public static final int FLAG_SLIM = 8;
        public static final int FLAG_PROPORTIONS = 16;
        public static final int FLAG_GENDER = 32;
        public static final int FLAG_FORM = 64;
        public static final int FLAG_SHOW_NAME = 128;
        public static final int FLAG_SHOW_ARMOR = 256;

        public static final Type<SettingsPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "companion_settings"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC =
                StreamCodec.of(SettingsPayload::write, SettingsPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, SettingsPayload p) {
            buf.writeVarInt(p.entityId);
            buf.writeUtf(p.name == null ? "" : p.name, 64);
            buf.writeFloat(p.scale);
            buf.writeUtf(p.skinPath == null ? "" : p.skinPath, 256);
            buf.writeBoolean(p.slimArms);
            buf.writeBoolean(p.male);
            buf.writeFloat(p.bust);
            buf.writeFloat(p.waist);
            buf.writeFloat(p.hips);
            buf.writeFloat(p.shoulders);
            buf.writeFloat(p.bustOffset);
            buf.writeUtf(p.form == null ? "player" : p.form, 32);
            buf.writeBoolean(p.showNameTag);
            buf.writeBoolean(p.showArmor);
            buf.writeVarInt(p.flags);
        }

        private static SettingsPayload read(RegistryFriendlyByteBuf buf) {
            return new SettingsPayload(
                    buf.readVarInt(), buf.readUtf(64), buf.readFloat(), buf.readUtf(256),
                    buf.readBoolean(), buf.readBoolean(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readUtf(32), buf.readBoolean(), buf.readBoolean(),
                    buf.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ContextSkinsPayload(
            int entityId,
            String sleepingSkin,
            String bathingSkin,
            String adventuringSkin
    ) implements CustomPacketPayload {
        public static final Type<ContextSkinsPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "companion_context_skins"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ContextSkinsPayload> CODEC =
                StreamCodec.of(ContextSkinsPayload::write, ContextSkinsPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, ContextSkinsPayload p) {
            int max = CompanionContextSkinSupport.MAX_PATH_LENGTH;
            buf.writeVarInt(p.entityId);
            buf.writeUtf(p.sleepingSkin == null ? "" : p.sleepingSkin, max);
            buf.writeUtf(p.bathingSkin == null ? "" : p.bathingSkin, max);
            buf.writeUtf(p.adventuringSkin == null ? "" : p.adventuringSkin, max);
        }

        private static ContextSkinsPayload read(RegistryFriendlyByteBuf buf) {
            int max = CompanionContextSkinSupport.MAX_PATH_LENGTH;
            return new ContextSkinsPayload(
                    buf.readVarInt(), buf.readUtf(max), buf.readUtf(max), buf.readUtf(max));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OrbSettingsPayload(
            int entityId,
            int colorRgb,
            int brightness,
            float floatAmplitude,
            float floatSpeed,
            float floatHeight,
            float offsetX,
            float offsetY,
            float offsetZ,
            boolean front
    ) implements CustomPacketPayload {
        public static final Type<OrbSettingsPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "companion_orb_settings"));

        public static final StreamCodec<RegistryFriendlyByteBuf, OrbSettingsPayload> CODEC =
                StreamCodec.of(OrbSettingsPayload::write, OrbSettingsPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, OrbSettingsPayload p) {
            buf.writeVarInt(p.entityId);
            buf.writeInt(p.colorRgb);
            buf.writeVarInt(p.brightness);
            buf.writeFloat(p.floatAmplitude);
            buf.writeFloat(p.floatSpeed);
            buf.writeFloat(p.floatHeight);
            buf.writeFloat(p.offsetX);
            buf.writeFloat(p.offsetY);
            buf.writeFloat(p.offsetZ);
            buf.writeBoolean(p.front);
        }

        private static OrbSettingsPayload read(RegistryFriendlyByteBuf buf) {
            return new OrbSettingsPayload(
                    buf.readVarInt(),
                    buf.readInt(),
                    buf.readVarInt(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BehaviorPayload(
            int entityId,
            float followRadius,
            float personalSpace,
            float wanderRadius
    ) implements CustomPacketPayload {
        public static final Type<BehaviorPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "companion_behavior"));

        public static final StreamCodec<RegistryFriendlyByteBuf, BehaviorPayload> CODEC =
                StreamCodec.of(BehaviorPayload::write, BehaviorPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, BehaviorPayload p) {
            buf.writeVarInt(p.entityId);
            buf.writeFloat(p.followRadius);
            buf.writeFloat(p.personalSpace);
            buf.writeFloat(p.wanderRadius);
        }

        private static BehaviorPayload read(RegistryFriendlyByteBuf buf) {
            return new BehaviorPayload(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
