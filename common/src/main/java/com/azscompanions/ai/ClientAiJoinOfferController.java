package com.azscompanions.ai;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loader-agnostic join-time LLM consent orchestration (no Minecraft types).
 * Loaders supply UI open + packet send callbacks.
 */
public final class ClientAiJoinOfferController {
    private static final int DELAY_TICKS = 40;

    private static int delayLeft = -1;
    private static AiJoinOffer delayedOffer;
    private static Consumer<AiJoinOffer> openPrompt = o -> {
    };
    private static BiConsumer<Boolean, AiJoinOffer> sendConsent = (a, o) -> {
    };
    private static Supplier<Boolean> screenBusy = () -> false;
    private static Executor clientExecutor = Runnable::run;

    private ClientAiJoinOfferController() {
    }

    public static void configure(
            Consumer<AiJoinOffer> openPromptUi,
            BiConsumer<Boolean, AiJoinOffer> consentSender,
            Supplier<Boolean> isScreenBusy,
            Executor runOnClient
    ) {
        openPrompt = openPromptUi == null ? o -> {
        } : openPromptUi;
        sendConsent = consentSender == null ? (a, o) -> {
        } : consentSender;
        screenBusy = isScreenBusy == null ? () -> false : isScreenBusy;
        clientExecutor = runOnClient == null ? Runnable::run : runOnClient;
    }

    public static void onDisconnected() {
        ClientAiJoinConsent.endConnection();
        delayLeft = -1;
        delayedOffer = null;
    }

    public static void handleServerOffer(AiJoinOffer offer, String serverKey) {
        ClientAiJoinConsent.beginConnection(serverKey);
        if (offer == null) {
            return;
        }
        if (offer.available()) {
            schedulePrompt(offer);
            return;
        }
        if (offer.allowLocalProbe() && ClientAiJoinConsent.shouldPrompt(serverKey)) {
            CompletableFuture.supplyAsync(() -> LlmEndpointProbe.probeLocal(new CompanionAiSettings()))
                    .thenAccept(hit -> clientExecutor.execute(() -> {
                        if (hit.isEmpty()) {
                            return;
                        }
                        if (!ClientAiJoinConsent.shouldPrompt(ClientAiJoinConsent.currentServerKey())) {
                            return;
                        }
                        schedulePrompt(AiJoinOffer.fromLocalProbe(hit.get()));
                    }));
        }
    }

    public static void clientTick() {
        if (delayLeft < 0) {
            return;
        }
        if (--delayLeft > 0) {
            return;
        }
        AiJoinOffer offer = delayedOffer;
        delayedOffer = null;
        delayLeft = -1;
        if (offer == null || !offer.available()) {
            return;
        }
        String key = ClientAiJoinConsent.currentServerKey();
        if (!ClientAiJoinConsent.shouldPrompt(key)) {
            return;
        }
        if (Boolean.TRUE.equals(screenBusy.get())) {
            delayedOffer = offer;
            delayLeft = 20;
            return;
        }
        ClientAiJoinConsent.setPendingOffer(offer);
        openPrompt.accept(offer);
    }

    public static void onUserChoice(boolean accepted) {
        String key = ClientAiJoinConsent.currentServerKey();
        AiJoinOffer offer = ClientAiJoinConsent.pendingOffer();
        if (accepted) {
            ClientAiJoinConsent.markAccepted(key);
        } else {
            ClientAiJoinConsent.markDismissed(key);
        }
        if (offer != null) {
            sendConsent.accept(accepted, offer);
        }
        ClientAiJoinConsent.clearPendingOffer();
    }

    private static void schedulePrompt(AiJoinOffer offer) {
        String key = ClientAiJoinConsent.currentServerKey();
        if (!ClientAiJoinConsent.shouldPrompt(key) || offer == null || !offer.available()) {
            return;
        }
        delayedOffer = offer;
        delayLeft = DELAY_TICKS;
    }

    /** Test helper. */
    static Optional<AiJoinOffer> peekDelayed() {
        return Optional.ofNullable(delayedOffer);
    }
}
