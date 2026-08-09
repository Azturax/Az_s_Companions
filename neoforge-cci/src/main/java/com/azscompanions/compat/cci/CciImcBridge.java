package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Polls NeoForge {@link InterModComms} for runtime messages from CCI's {@code IMCOutcome}.
 *
 * <p>Subjects (method keys):
 * <ul>
 *   <li>{@code companion_say} — companion says {@code message} to owner</li>
 *   <li>{@code companion_greet} — companion greets subscriber name in {@code message}</li>
 *   <li>{@code companion_follow} — set follow mode</li>
 *   <li>{@code companion_sit} / {@code companion_stay} — sit/stay</li>
 *   <li>{@code companion_wave} — wave / hello line including {@code message}</li>
 * </ul>
 */
public final class CciImcBridge {
    private static int serverTickCounter;

    private CciImcBridge() {
    }

    static void bootstrap() {
        AzsCompanions.LOGGER.info("CCI IMC bridge listening for subjects companion_say/greet/follow/sit/stay/wave");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CciImcBridgeClient.register();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Integrated client uses the client poller; only dedicated server ticks here.
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

        // Dedicated server path (rare for CCI IMC, which is documented client-side).
        CciCompanionActions.applyOnServer(null, action, safeMessage);
    }

    /** Used by client bridge after validating a local player exists. */
    static void sendToServer(CciCompanionAction action, String message) {
        PacketDistributor.sendToServer(new CciActionPacket(action.name(), message == null ? "" : message));
    }
}
