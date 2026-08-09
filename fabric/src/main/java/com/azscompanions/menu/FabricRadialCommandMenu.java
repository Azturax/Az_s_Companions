package com.azscompanions.menu;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.registry.FabricModScreenHandlers;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Legacy radial menu; interact entry removed. Commands force follow-only. */
public final class FabricRadialCommandMenu extends AbstractContainerMenu {
    public enum Command {
        FOLLOW, STAY, GATHER, STOP_TASK, OPEN_INVENTORY
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
        companion.setMode(FabricCompanionMode.FOLLOW);
        companion.getTaskQueue().cancelActive();
        if (command == Command.OPEN_INVENTORY && player instanceof ServerPlayer serverPlayer) {
            companion.openInventory(serverPlayer);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return companion != null && companion.isAlive() && companion.distanceTo(player) < 8.0d;
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
