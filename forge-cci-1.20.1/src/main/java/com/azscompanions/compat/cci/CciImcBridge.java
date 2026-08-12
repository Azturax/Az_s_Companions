package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import com.azscompanions.network.ModNetworking;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;

/**
 * Polls Forge {@link InterModComms} for runtime messages from CCI's IMCOutcome.
 */
public final class CciImcBridge {
    private static int serverTickCounter;

    private CciImcBridge() {
    }

    static void bootstrap() {
        AzsCompanions.LOGGER.info(
                "CCI IMC bridge listening for subjects say/greet/wave/follow/sit/stay/modify/persona/play/rush/hide_seek/claim_chunk/ai_config/ask/teamfight*/spawn*");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CciImcBridgeClient.register();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return;
        }
        if ((++serverTickCounter % 5) != 0) {
            return;
        }
        drainQueuedMessages("server");
    }

    static void drainQueuedMessages(String source) {
        InterModComms.getMessages(AzsCompanions.MOD_ID).forEach(msg -> {
            String subject = msg.method();
            Object payload = msg.messageSupplier().get();
            String message = payload == null ? "" : String.valueOf(payload);
            AzsCompanions.LOGGER.debug("CCI IMC [{}] from={} subject={} message={}",
                    source, msg.senderModId(), subject, message);
            dispatch(subject, message);
        });
    }

    static void dispatch(String subject, String message) {
        CciCompanionAction action = CciCompanionAction.fromSubject(subject);
        if (action == null) {
            AzsCompanions.LOGGER.warn("Unknown CCI IMC subject '{}' (ignored)", subject);
            return;
        }

        String safeMessage = message == null ? "" : message;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CciImcBridgeClient.sendActionToServer(action, safeMessage);
            return;
        }

        CciCompanionActions.applyOnServer(null, action, safeMessage);
    }

    /** Used by client bridge after validating a local player exists. */
    static void sendToServer(CciCompanionAction action, String message) {
        ModNetworking.CHANNEL.send(
                PacketDistributor.SERVER.noArg(),
                new CciActionPacket(action.name(), message == null ? "" : message));
    }
}
