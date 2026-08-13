package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import com.azscompanions.network.ModNetworking;
import me.ichun.mods.cci.api.CCIApi;
import me.ichun.mods.cci.api.IApi;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.network.NetworkDirection;

/**
 * CCI soft-compat bootstrap for Forge 1.20.1. Loaded only when CCI is present
 * (see {@link CciCompatModule}).
 */
public final class CciCompatBootstrap {
    private static boolean registeredPacket;

    private CciCompatBootstrap() {
    }

    /** Called reflectively from {@link CciCompatModule} when CCI is loaded. */
    public static void register(IEventBus modBus) {
        modBus.addListener(CciCompatBootstrap::onCommonSetup);
        modBus.addListener(CciCompatBootstrap::onImcProcess);
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            registerCciPacket();
            IApi api = CCIApi.getApiImpl();
            AzsCompanions.LOGGER.info(
                    "CCI soft-compat active — Content Creator Integration API present={}",
                    api != null && !(api.getClass().getSimpleName().equals("ApiDummy")));
            MinecraftForge.EVENT_BUS.register(CciImcBridge.class);
            CciImcBridge.bootstrap();
        });
    }

    @SubscribeEvent
    public static void onImcProcess(InterModProcessEvent event) {
        CciImcBridge.drainQueuedMessages("InterModProcessEvent");
    }

    private static void registerCciPacket() {
        if (registeredPacket) {
            return;
        }
        registeredPacket = true;
        ModNetworking.CHANNEL.messageBuilder(CciActionPacket.class, 64, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CciActionPacket::encode)
                .decoder(CciActionPacket::decode)
                .consumerMainThread(CciActionPacket::handle)
                .add();
    }
}
