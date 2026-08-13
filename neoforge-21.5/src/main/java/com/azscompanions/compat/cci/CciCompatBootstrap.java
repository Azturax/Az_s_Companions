package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import me.ichun.mods.cci.api.CCIApi;
import me.ichun.mods.cci.api.IApi;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * CCI soft-compat bootstrap. Loaded only when Content Creator Integration is present
 * (see {@link CciCompatModule}). Hooks CCI via {@link CCIApi} + runtime IMC.
 */
public final class CciCompatBootstrap {
    private CciCompatBootstrap() {
    }

    /** Called reflectively from {@link CciCompatModule} when CCI is loaded. */
    public static void register(IEventBus modBus) {
        modBus.addListener(CciCompatBootstrap::onCommonSetup);
        modBus.addListener(CciCompatBootstrap::onImcProcess);
        modBus.addListener(CciCompatBootstrap::registerPayloads);
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            IApi api = CCIApi.getApiImpl();
            AzsCompanions.LOGGER.info(
                    "CCI soft-compat active — Content Creator Integration API present={}",
                    api != null && !(api.getClass().getSimpleName().equals("ApiDummy")));
            NeoForge.EVENT_BUS.register(CciImcBridge.class);
            CciImcBridge.bootstrap();
        });
    }

    @SubscribeEvent
    public static void onImcProcess(InterModProcessEvent event) {
        CciImcBridge.drainQueuedMessages("InterModProcessEvent");
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AzsCompanions.MOD_ID).versioned("1");
        registrar.playToServer(CciActionPacket.TYPE, CciActionPacket.STREAM_CODEC, CciActionPacket::handle);
    }
}
