package com.azscompanions.menu;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.registry.FabricModScreenHandlers;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Simple companion command radial — Follow / Stay / Wander / Emotes. */
public final class FabricRadialCommandMenu extends AbstractContainerMenu {
    public enum Command {
        FOLLOW,
        STAY,
        WANDER,
        EMOTE_WAVE,
        EMOTE_CHEER,
        OPEN_INVENTORY
    }

    private final FabricCompanionEntity companion;

    public FabricRadialCommandMenu(int syncId, Inventory inv, int entityId) {
        super(FabricModScreenHandlers.RADIAL, syncId);
        var entity = inv.player.level().getEntity(entityId);
        this.companion = entity instanceof FabricCompanionEntity c ? c : null;
    }

    public FabricCompanionEntity companion() {
        return companion;
    }

    public void runCommand(Player player, Command command) {
        if (companion == null || !companion.isOwnedBy(player)) {
            return;
        }
        if (companion.distanceTo(player) > 64.0d) {
            return;
        }
        switch (command) {
            case FOLLOW -> {
                companion.getTaskQueue().cancelActive();
                companion.setMode(FabricCompanionMode.FOLLOW);
            }
            case STAY -> {
                companion.getTaskQueue().cancelActive();
                companion.setMode(FabricCompanionMode.STAY);
            }
            case WANDER -> {
                companion.getTaskQueue().cancelActive();
                companion.setMode(FabricCompanionMode.WANDER);
            }
            case EMOTE_WAVE -> {
                companion.swing(InteractionHand.MAIN_HAND, true);
                companion.speakGreeting();
            }
            case EMOTE_CHEER -> {
                companion.swing(InteractionHand.MAIN_HAND, true);
                companion.speakSuccess();
                if (companion.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.HEART,
                            companion.getX(),
                            companion.getY() + companion.getBbHeight() * 0.9d,
                            companion.getZ(),
                            6,
                            0.35d, 0.25d, 0.35d,
                            0.02d);
                }
            }
            case OPEN_INVENTORY -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    companion.openInventory(serverPlayer);
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return companion != null && companion.isAlive() && companion.distanceTo(player) < 64.0d;
    }

    public static final class ExtendedProvider implements ExtendedScreenHandlerFactory<Integer> {
        private final FabricCompanionEntity companion;

        public ExtendedProvider(FabricCompanionEntity companion) {
            this.companion = companion;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.azscompanions.radial");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new FabricRadialCommandMenu(id, inv, companion.getId());
        }

        @Override
        public Integer getScreenOpeningData(ServerPlayer player) {
            return companion.getId();
        }
    }
}
