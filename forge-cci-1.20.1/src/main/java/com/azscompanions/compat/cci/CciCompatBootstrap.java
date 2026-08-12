package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import com.azscompanions.network.ModNetworking;
import me.ichun.mods.cci.api.CCIApi;
import me.ichun.mods.cci.api.IApi;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.network.NetworkDirection;

/**
 * CCI-edition bootstrap for Forge 1.20.1.
 * Hooks iChun Content Creator Integration via {@link CCIApi} + runtime IMC.
 */
@Mod.EventBusSubscriber(modid = AzsCompanions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CciCompatBootstrap {
    private static boolean registeredPacket;

    private CciCompatBootstrap() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            registerCciPacket();
            IApi api = CCIApi.getApiImpl();
            AzsCompanions.LOGGER.info(
                    "CCI edition active — Content Creator Integration API present={}",
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
        // High message id reserved for CCI edition overlay.
        ModNetworking.CHANNEL.messageBuilder(CciActionPacket.class, 64, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CciActionPacket::encode)
                .decoder(CciActionPacket::decode)
                .consumerMainThread(CciActionPacket::handle)
                .add();
    }
}
