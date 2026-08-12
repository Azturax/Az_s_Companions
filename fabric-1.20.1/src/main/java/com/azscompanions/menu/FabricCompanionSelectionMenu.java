package com.azscompanions.menu;

import com.azscompanions.entity.FabricCompanionDefinition;
import com.azscompanions.entity.FabricCompanionRegistry;
import com.azscompanions.registry.FabricModScreenHandlers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class FabricCompanionSelectionMenu extends AbstractContainerMenu {
    private final List<FabricCompanionDefinition> options = new ArrayList<>();

    public FabricCompanionSelectionMenu(int syncId, Inventory inv) {
        super(FabricModScreenHandlers.SELECTION, syncId);
        options.addAll(FabricCompanionRegistry.all());
    }

    /** Extended factory unused payload. */
    public FabricCompanionSelectionMenu(int syncId, Inventory inv, Void ignored) {
        this(syncId, inv);
    }

    public List<FabricCompanionDefinition> options() {
        return List.copyOf(options);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
