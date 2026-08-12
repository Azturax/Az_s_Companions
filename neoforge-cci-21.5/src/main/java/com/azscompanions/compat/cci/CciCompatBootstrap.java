package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import me.ichun.mods.cci.api.CCIApi;
import me.ichun.mods.cci.api.IApi;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * CCI-edition bootstrap. Only compiled into {@code azscompanions-neoforge-cci}.
 * Hooks iChun Content Creator Integration via {@link CCIApi} + runtime IMC.
 */
@EventBusSubscriber(modid = AzsCompanions.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CciCompatBootstrap {
    private CciCompatBootstrap() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            IApi api = CCIApi.getApiImpl();
            AzsCompanions.LOGGER.info(
                    "CCI edition active — Content Creator Integration API present={}",
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
