package com.azscompanions.menu;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.azscompanions.registry.FabricModScreenHandlers;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class FabricCompanionInventoryMenu extends AbstractContainerMenu {
    private final FabricCompanionEntity companion;

    public FabricCompanionInventoryMenu(int syncId, Inventory playerInv, int entityId) {
        super(FabricModScreenHandlers.INVENTORY, syncId);
        var entity = playerInv.player.level().getEntity(entityId);
        this.companion = entity instanceof FabricCompanionEntity c ? c : null;
        if (companion == null) {
            return;
        }
        FabricCompanionInventory inv = companion.getCompanionInventory();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        addSlot(new Slot(inv, FabricCompanionInventory.MAIN_HAND, 8, 86));
        addSlot(new Slot(inv, FabricCompanionInventory.OFF_HAND, 26, 86));
        addSlot(new Slot(inv, FabricCompanionInventory.HEAD, 62, 86));
        addSlot(new Slot(inv, FabricCompanionInventory.CHEST, 80, 86));
        addSlot(new Slot(inv, FabricCompanionInventory.LEGS, 98, 86));
        addSlot(new Slot(inv, FabricCompanionInventory.FEET, 116, 86));
        addSlot(new Slot(inv, FabricCompanionInventory.FOOD, 152, 86));

        for (int i = 0; i < FabricCompanionInventory.COSMETIC_SLOTS; i++) {
            addSlot(new Slot(inv, FabricCompanionInventory.COSMETIC_START + i, 62 + i * 18, 108));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 136 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 194));
        }
    }

    public FabricCompanionEntity companion() {
        return companion;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int companionSlots = FabricCompanionInventory.TOTAL_SIZE;
            if (index < companionSlots) {
                if (!moveItemStackTo(stack, companionSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, FabricCompanionInventory.BACKPACK_SIZE, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return companion != null && companion.isAlive() && companion.isOwnedBy(player)
                && companion.distanceTo(player) < 8.0d;
    }

    public static final class ExtendedProvider implements ExtendedScreenHandlerFactory<Integer> {
        private final FabricCompanionEntity companion;

        public ExtendedProvider(FabricCompanionEntity companion) {
            this.companion = companion;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.azscompanions.inventory");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new FabricCompanionInventoryMenu(id, inv, companion.getId());
        }

        @Override
        public Integer getScreenOpeningData(ServerPlayer player) {
            return companion.getId();
        }
    }
}
