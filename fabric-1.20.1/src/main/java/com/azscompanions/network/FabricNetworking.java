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
import com.azscompanions.entity.FabricCompanionPlayerDataSupport;
import com.azscompanions.entity.FabricCompanionRecruitment;
import com.azscompanions.item.FabricCharmData;
import com.azscompanions.item.FabricCompanionCharmItem;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class FabricNetworking {
    private FabricNetworking() {
    }

    public static void sendToPlayer(ServerPlayer player, ResourceLocation id, java.util.function.Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        writer.accept(buf);
        ServerPlayNetworking.send(player, id, buf);
    }


    public static void register() {



















        ServerPlayNetworking.registerGlobalReceiver(RecruitPayload.ID, (server, player, handler, buf, responseSender) -> {
            RecruitPayload payload = RecruitPayload.read(buf);
            server.execute(() -> {
                    FabricCompanionEntity created = FabricCompanionRecruitment.recruitEntity(
                            player, payload.definitionId());
                    if (created != null) {
                        com.azscompanions.ai.FabricCompanionPersonaOnboarding.offerIfNeeded(player, created);
                    }
                });
        });

        ServerPlayNetworking.registerGlobalReceiver(MenuActionPayload.ID, (server, player, handler, buf, responseSender) -> {
            MenuActionPayload payload = MenuActionPayload.read(buf);
            server.execute(() -> {
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
                            FabricCompanionPlayerDataSupport.save(companion);
                            toastMode(player, companion, "message.azscompanions.mode_follow");
                        }
                        case "STAY" -> {
                            companion.setMode(FabricCompanionMode.STAY);
                            FabricCompanionPlayerDataSupport.save(companion);
                            toastMode(player, companion, "message.azscompanions.mode_stay");
                        }
                        case "SIT" -> {
                            companion.setMode(FabricCompanionMode.SIT);
                            FabricCompanionPlayerDataSupport.save(companion);
                            toastMode(player, companion, "message.azscompanions.mode_sit");
                        }
                        case "WANDER" -> {
                            companion.setMode(FabricCompanionMode.WANDER);
                            FabricCompanionPlayerDataSupport.save(companion);
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
                });
        });

        ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.ID, (server, player, handler, buf, responseSender) -> {
            SettingsPayload payload = SettingsPayload.read(buf);
            server.execute(() -> {
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
                    if ((payload.flags() & SettingsPayload.FLAG_FORM_VARIANT) != 0) {
                        companion.setFormVariant(payload.formVariant());
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
                    FabricCompanionPlayerDataSupport.save(companion);
                });
        });

        ServerPlayNetworking.registerGlobalReceiver(ContextSkinsPayload.ID, (server, player, handler, buf, responseSender) -> {
            ContextSkinsPayload payload = ContextSkinsPayload.read(buf);
            server.execute(() -> {
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
                    FabricCompanionPlayerDataSupport.save(companion);
                });
        });


        ServerPlayNetworking.registerGlobalReceiver(BehaviorPayload.ID, (server, player, handler, buf, responseSender) -> {
            BehaviorPayload payload = BehaviorPayload.read(buf);
            server.execute(() -> {
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
                    FabricCompanionPlayerDataSupport.save(companion);
                });
        });

        ServerPlayNetworking.registerGlobalReceiver(PersonaPayload.ID, (server, player, handler, buf, responseSender) -> {
            PersonaPayload payload = PersonaPayload.read(buf);
            server.execute(() -> {
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
                    com.azscompanions.entity.FabricCompanionDimensionTravelSupport.rememberIdentity(player, companion);
                    FabricCompanionPlayerDataSupport.save(companion);
                    player.displayClientMessage(Component.literal(
                            companion.getChatDisplayName() + " — persona "
                                    + (payload.skip() ? "skipped (defaults)" : "saved")), true);
                });
        });

        ServerPlayNetworking.registerGlobalReceiver(AdminAiSavePayload.ID, (server, player, handler, buf, responseSender) -> {
            AdminAiSavePayload payload = AdminAiSavePayload.read(buf);
            server.execute(() -> { com.azscompanions.admin.FabricAzAdminActions.saveAiConfig(
                        player,
                        com.azscompanions.admin.AdminAiConfigSnapshot.fromWireJson(payload.json())); });
        });

        ServerPlayNetworking.registerGlobalReceiver(AdminActionPayload.ID, (server, player, handler, buf, responseSender) -> {
            AdminActionPayload payload = AdminActionPayload.read(buf);
            server.execute(() -> { com.azscompanions.admin.FabricAzAdminActions.handleAction(player, payload.action()); });
        });

        ServerPlayNetworking.registerGlobalReceiver(DepositExitPayload.ID, (server, player, handler, buf, responseSender) -> {
            DepositExitPayload payload = DepositExitPayload.read(buf);
            server.execute(() -> {
                                        com.azscompanions.deposit.DepositChestSelection sel =
                            com.azscompanions.deposit.DepositChestSelection.of(player.getUUID());
                    if (!sel.isSelecting()) {
                        return;
                    }
                    sel.finishKeepingSelection();
                    com.azscompanions.deposit.FabricDepositCommands.sync(player, sel);
                    player.displayClientMessage(Component.translatable(
                            "message.azscompanions.deposit_done", sel.size()), true);
                });
        });

        ServerPlayNetworking.registerGlobalReceiver(AiJoinConsentPayload.ID, (server, player, handler, buf, responseSender) -> {
            AiJoinConsentPayload payload = AiJoinConsentPayload.read(buf);
            server.execute(() -> { com.azscompanions.ai.FabricAiJoinOfferEvents.handleConsent(
                                player,
                                payload.accepted(),
                                payload.suggestProfile(),
                                payload.applyProfile()); });
        });

        ServerPlayNetworking.registerGlobalReceiver(ToggleWigglyDogPayload.ID, (server, player, handler, buf, responseSender) -> {
            ToggleWigglyDogPayload payload = ToggleWigglyDogPayload.read(buf);
            server.execute(() -> {
                                        if (!com.azscompanions.perk.WigglyDogPerkSupport.isEligible(player.getUUID())) {
                        player.displayClientMessage(
                                Component.translatable("message.azscompanions.wiggly_dog_denied"), true);
                        return;
                    }
                    com.azscompanions.perk.WigglyDogPerk.toggle(player);
                });
        });
    }

    public static void openMenu(ServerPlayer player, FabricCompanionEntity companion) {
        sendToPlayer(player, OpenMenuPayload.ID, buf -> OpenMenuPayload.write(buf, new OpenMenuPayload(companion.getId())));
    }

    public static void openPersonaSetup(ServerPlayer player, FabricCompanionEntity companion) {
        var p = companion.getPersona();
        sendToPlayer(player, OpenPersonaPayload.ID, buf -> OpenPersonaPayload.write(buf, new OpenPersonaPayload(
                companion.getId(),
                p.whoAmI(),
                p.whatAmIDoing(),
                p.howWillIBe(),
                p.speechStyle(),
                p.relationshipToOwner(),
                p.quirks())));
    }

    public static void openStats(ServerPlayer player, FabricCompanionEntity companion) {
        var p = companion.getPersona();
        sendToPlayer(player, OpenStatsPayload.ID, buf -> OpenStatsPayload.write(buf, new OpenStatsPayload(
                companion.getId(),
                CompanionStatsText.personaSnippet(p.whoAmI()),
                CompanionStatsText.personaSnippet(p.whatAmIDoing()),
                CompanionStatsText.personaSnippet(p.howWillIBe()),
                FabricCompanionRecruitment.countChildrenOf(player, companion.getUUID()),
                (int) FabricCompanionRecruitment.countOwned(player),
                resolveCharmStatus(player),
                CompanionStatsText.aiStatusIfEnabled())));
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
        sendToPlayer(player, OpenAdminPayload.ID, buf -> OpenAdminPayload.write(buf, new OpenAdminPayload(
                json,
                aiStatus == null ? "" : aiStatus,
                chunkLoading,
                teamfight,
                companionSummary == null ? "" : companionSummary)));
    }

    public static void sendTeamFightHud(ServerPlayer player, String encodedSnapshot) {
        sendToPlayer(player, TeamFightHudPayload.ID, buf -> TeamFightHudPayload.write(buf, new TeamFightHudPayload(encodedSnapshot == null ? "" : encodedSnapshot)));
    }

    public static void sendAiThinking(ServerPlayer player, boolean active, String companionName,
                                      int timeoutSeconds, float progress) {
        sendToPlayer(player, AiThinkingPayload.ID, buf -> AiThinkingPayload.write(buf, new AiThinkingPayload(
                active,
                companionName == null ? "" : companionName,
                timeoutSeconds,
                progress)));
    }

    public static void sendAiJoinOffer(ServerPlayer player, com.azscompanions.ai.AiJoinOffer offer) {
        com.azscompanions.ai.AiJoinOffer o = offer == null
                ? com.azscompanions.ai.AiJoinOffer.none()
                : offer;
        sendToPlayer(player, AiJoinOfferPayload.ID, buf -> AiJoinOfferPayload.write(buf, new AiJoinOfferPayload(
                o.available(),
                o.source(),
                o.providerLabel(),
                o.endpointHint(),
                o.suggestProfile(),
                o.allowApply(),
                o.allowLocalProbe())));
    }

    private static void toastMode(ServerPlayer player, FabricCompanionEntity companion, String key) {
        player.displayClientMessage(
                Component.literal(companion.getChatDisplayName() + " — ")
                        .append(Component.translatable(key)),
                true);
    }

    public record OpenMenuPayload(int entityId) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "open_menu");

        public static void write(FriendlyByteBuf buf, OpenMenuPayload p) { buf.writeVarInt(p.entityId); }
        public static OpenMenuPayload read(FriendlyByteBuf buf) { return new OpenMenuPayload(buf.readVarInt()); }
    }

    public record OpenPersonaPayload(
            int entityId,
            String whoAmI,
            String whatAmIDoing,
            String howWillIBe,
            String speechStyle,
            String relationshipToOwner,
            String quirks
    ) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "open_persona");

        public static void write(FriendlyByteBuf buf, OpenPersonaPayload p) {
            buf.writeVarInt(p.entityId);
            int max = com.azscompanions.ai.CompanionPersona.MAX_LEN;
            buf.writeUtf(p.whoAmI == null ? "" : p.whoAmI, max);
            buf.writeUtf(p.whatAmIDoing == null ? "" : p.whatAmIDoing, max);
            buf.writeUtf(p.howWillIBe == null ? "" : p.howWillIBe, max);
            buf.writeUtf(p.speechStyle == null ? "" : p.speechStyle, max);
            buf.writeUtf(p.relationshipToOwner == null ? "" : p.relationshipToOwner, max);
            buf.writeUtf(p.quirks == null ? "" : p.quirks, max);
        }

        public static OpenPersonaPayload read(FriendlyByteBuf buf) {
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
    ) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "open_stats");

        public static void write(FriendlyByteBuf buf, OpenStatsPayload p) {
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

        public static OpenStatsPayload read(FriendlyByteBuf buf) {
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
    }

    public record OpenAdminPayload(
            String aiJson,
            String aiStatus,
            boolean chunkLoading,
            boolean teamfight,
            String companionSummary
    ) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "open_admin");

        public static void write(FriendlyByteBuf buf, OpenAdminPayload p) {
            buf.writeUtf(p.aiJson == null ? "{}" : p.aiJson, com.azscompanions.admin.AdminAiConfigSnapshot.MAX_WIRE_JSON);
            buf.writeUtf(p.aiStatus == null ? "" : p.aiStatus, 512);
            buf.writeBoolean(p.chunkLoading);
            buf.writeBoolean(p.teamfight);
            buf.writeUtf(p.companionSummary == null ? "" : p.companionSummary, 1024);
        }

        public static OpenAdminPayload read(FriendlyByteBuf buf) {
            return new OpenAdminPayload(
                    buf.readUtf(com.azscompanions.admin.AdminAiConfigSnapshot.MAX_WIRE_JSON),
                    buf.readUtf(512),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readUtf(1024));
        }
    }

    public record AdminAiSavePayload(String json) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "admin_ai_save");

        public static void write(FriendlyByteBuf buf, AdminAiSavePayload p) {
            buf.writeUtf(p.json == null ? "{}" : p.json, com.azscompanions.admin.AdminAiConfigSnapshot.MAX_WIRE_JSON);
        }

        public static AdminAiSavePayload read(FriendlyByteBuf buf) {
            return new AdminAiSavePayload(buf.readUtf(com.azscompanions.admin.AdminAiConfigSnapshot.MAX_WIRE_JSON));
        }
    }

    public record AdminActionPayload(String action) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "admin_action");

        public static void write(FriendlyByteBuf buf, AdminActionPayload p) { buf.writeUtf(p.action == null ? "" : p.action, 64); }
        public static AdminActionPayload read(FriendlyByteBuf buf) { return new AdminActionPayload(buf.readUtf(64)); }
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
    ) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "companion_persona");

        public static void write(FriendlyByteBuf buf, PersonaPayload p) {
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

        public static PersonaPayload read(FriendlyByteBuf buf) {
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
    }

    public record TeamFightHudPayload(String payload) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "teamfight_hud");

        public static void write(FriendlyByteBuf buf, TeamFightHudPayload p) { buf.writeUtf(p.payload == null ? "" : p.payload, 8192); }
        public static TeamFightHudPayload read(FriendlyByteBuf buf) { return new TeamFightHudPayload(buf.readUtf(8192)); }
    }

    public record AiThinkingPayload(
            boolean active,
            String companionName,
            int timeoutSeconds,
            float progress
    ) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "companion_ai_thinking");

        public static void write(FriendlyByteBuf buf, AiThinkingPayload p) {
            buf.writeBoolean(p.active);
            buf.writeUtf(p.companionName == null ? "" : p.companionName, 64);
            buf.writeVarInt(p.timeoutSeconds);
            buf.writeFloat(p.progress);
        }
        public static AiThinkingPayload read(FriendlyByteBuf buf) {
            return new AiThinkingPayload(buf.readBoolean(), buf.readUtf(64), buf.readVarInt(), buf.readFloat());
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
    ) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "ai_join_offer");

        public static void write(FriendlyByteBuf buf, AiJoinOfferPayload p) {
            buf.writeBoolean(p.available);
            buf.writeUtf(p.source == null ? "" : p.source, 16);
            buf.writeUtf(p.providerLabel == null ? "" : p.providerLabel, 64);
            buf.writeUtf(p.endpointHint == null ? "" : p.endpointHint, 128);
            buf.writeUtf(p.suggestProfile == null ? "" : p.suggestProfile, 32);
            buf.writeBoolean(p.allowApply);
            buf.writeBoolean(p.allowLocalProbe);
        }

        public static AiJoinOfferPayload read(FriendlyByteBuf buf) {
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
    }

    public record AiJoinConsentPayload(
            boolean accepted,
            String suggestProfile,
            boolean applyProfile
    ) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "ai_join_consent");

        public static void write(FriendlyByteBuf buf, AiJoinConsentPayload p) {
            buf.writeBoolean(p.accepted);
            buf.writeUtf(p.suggestProfile == null ? "" : p.suggestProfile, 32);
            buf.writeBoolean(p.applyProfile);
        }

        public static AiJoinConsentPayload read(FriendlyByteBuf buf) {
            return new AiJoinConsentPayload(buf.readBoolean(), buf.readUtf(32), buf.readBoolean());
        }
    }

    public record DepositSelectionPayload(String payload) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "deposit_selection");

        public static void write(FriendlyByteBuf buf, DepositSelectionPayload p) { buf.writeUtf(p.payload == null ? "" : p.payload, 8192); }
        public static DepositSelectionPayload read(FriendlyByteBuf buf) { return new DepositSelectionPayload(buf.readUtf(8192)); }
    }

    public record DepositExitPayload() {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "deposit_exit");

        public static void write(FriendlyByteBuf buf, DepositExitPayload p) { }
        public static DepositExitPayload read(FriendlyByteBuf buf) { return new DepositExitPayload(); }
    }

    public record ToggleWigglyDogPayload() {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "toggle_wiggly_dog");

        public static void write(FriendlyByteBuf buf, ToggleWigglyDogPayload p) { }
        public static ToggleWigglyDogPayload read(FriendlyByteBuf buf) { return new ToggleWigglyDogPayload(); }
    }

    public record MenuActionPayload(int entityId, String action) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "menu_action");

        public static void write(FriendlyByteBuf buf, MenuActionPayload p) {
            buf.writeVarInt(p.entityId);
            buf.writeUtf(p.action == null ? "" : p.action, 64);
        }
        public static MenuActionPayload read(FriendlyByteBuf buf) {
            return new MenuActionPayload(buf.readVarInt(), buf.readUtf(64));
        }
    }

    public record RecruitPayload(String definitionId) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "recruit");

        public static void write(FriendlyByteBuf buf, RecruitPayload p) { buf.writeUtf(p.definitionId == null ? "" : p.definitionId, 256); }
        public static RecruitPayload read(FriendlyByteBuf buf) { return new RecruitPayload(buf.readUtf(256)); }
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
            String formVariant,
            int flags
    ) {
        public static final int FLAG_NAME = 1;
        public static final int FLAG_SCALE = 2;
        public static final int FLAG_SKIN = 4;
        public static final int FLAG_SLIM = 8;
        public static final int FLAG_PROPORTIONS = 16;
        public static final int FLAG_GENDER = 32;
        public static final int FLAG_FORM = 64;
        public static final int FLAG_SHOW_NAME = 128;
        public static final int FLAG_SHOW_ARMOR = 256;
        public static final int FLAG_FORM_VARIANT = 512;

        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "companion_settings");

        public static void write(FriendlyByteBuf buf, SettingsPayload p) {
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
            buf.writeUtf(p.formVariant == null ? "" : p.formVariant, 64);
            buf.writeVarInt(p.flags);
        }

        public static SettingsPayload read(FriendlyByteBuf buf) {
            return new SettingsPayload(
                    buf.readVarInt(), buf.readUtf(64), buf.readFloat(), buf.readUtf(256),
                    buf.readBoolean(), buf.readBoolean(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readUtf(32), buf.readBoolean(), buf.readBoolean(),
                    buf.readUtf(64),
                    buf.readVarInt());
        }
    }

    public record ContextSkinsPayload(
            int entityId,
            String sleepingSkin,
            String bathingSkin,
            String adventuringSkin
    ) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "companion_context_skins");

        public static void write(FriendlyByteBuf buf, ContextSkinsPayload p) {
            int max = CompanionContextSkinSupport.MAX_PATH_LENGTH;
            buf.writeVarInt(p.entityId);
            buf.writeUtf(p.sleepingSkin == null ? "" : p.sleepingSkin, max);
            buf.writeUtf(p.bathingSkin == null ? "" : p.bathingSkin, max);
            buf.writeUtf(p.adventuringSkin == null ? "" : p.adventuringSkin, max);
        }

        public static ContextSkinsPayload read(FriendlyByteBuf buf) {
            int max = CompanionContextSkinSupport.MAX_PATH_LENGTH;
            return new ContextSkinsPayload(
                    buf.readVarInt(), buf.readUtf(max), buf.readUtf(max), buf.readUtf(max));
        }
    }


    public record BehaviorPayload(
            int entityId,
            float followRadius,
            float personalSpace,
            float wanderRadius
    ) {
        public static final ResourceLocation ID = new ResourceLocation(AzsCompanionsFabric.MOD_ID, "companion_behavior");

        public static void write(FriendlyByteBuf buf, BehaviorPayload p) {
            buf.writeVarInt(p.entityId);
            buf.writeFloat(p.followRadius);
            buf.writeFloat(p.personalSpace);
            buf.writeFloat(p.wanderRadius);
        }

        public static BehaviorPayload read(FriendlyByteBuf buf) {
            return new BehaviorPayload(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }
    }
}
