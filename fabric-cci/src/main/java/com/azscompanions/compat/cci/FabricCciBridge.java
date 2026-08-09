package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanionsFabric;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric CCI bridge. Official IMCOutcome uses iChunUtil {@code sendIMCMessage}, which is a no-op
 * on Fabric; {@link com.azscompanions.mixin.cci.IMCOutcomeMixin} forwards matching messages here
 * so the same IMC subjects work as on NeoForge.
 *
 * <p>Also exposes {@code /azscci &lt;subject&gt; [message]} for CCI {@code CommandOutcome} as a
 * documented Fabric fallback.
 */
public final class FabricCciBridge {
    private static ClientSender clientSender;

    private FabricCciBridge() {
    }

    /** Registered from the client entrypoint only. */
    public static void setClientSender(ClientSender sender) {
        clientSender = sender;
    }

    public static void dispatch(String subject, String message) {
        FabricCciCompanionAction action = FabricCciCompanionAction.fromSubject(subject);
        if (action == null) {
            AzsCompanionsFabric.LOGGER.warn("Unknown CCI subject '{}' (ignored)", subject);
            return;
        }

        String safeMessage = message == null ? "" : message;
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            if (clientSender != null) {
                clientSender.send(action, safeMessage);
            } else {
                AzsCompanionsFabric.LOGGER.debug("CCI client sender not ready");
            }
            return;
        }

        // Dedicated server without player context cannot apply owner-scoped actions.
        AzsCompanionsFabric.LOGGER.debug("CCI dispatch on dedicated server ignored for subject {}", subject);
    }

    public static void dispatchForPlayer(ServerPlayer player, String subject, String message) {
        FabricCciCompanionAction action = FabricCciCompanionAction.fromSubject(subject);
        if (action == null) {
            AzsCompanionsFabric.LOGGER.warn("Unknown CCI subject '{}' (ignored)", subject);
            return;
        }
        FabricCciCompanionActions.applyOnServer(player, action, message == null ? "" : message);
    }

    @FunctionalInterface
    public interface ClientSender {
        void send(FabricCciCompanionAction action, String message);
    }
}
