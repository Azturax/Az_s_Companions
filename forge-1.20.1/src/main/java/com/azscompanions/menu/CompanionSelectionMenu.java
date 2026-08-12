package com.azscompanions.menu;

import com.azscompanions.entity.CompanionDefinition;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CompanionSelectionMenu extends AbstractContainerMenu {
    private final List<CompanionDefinition> options = new ArrayList<>();

    public CompanionSelectionMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv);
    }

    public CompanionSelectionMenu(int containerId, Inventory inv) {
        super(ModMenus.COMPANION_SELECTION.get(), containerId);
        options.addAll(CompanionRegistry.all());
        if (options.isEmpty()) {
            // Client may not have datapack yet; Kon is still the default selection id.
        }
    }

    public List<CompanionDefinition> options() {
        return List.copyOf(options);
    }

    public CompanionDefinition defaultSelection() {
        return CompanionRegistry.get(CompanionRegistry.KON_ID).orElse(options.isEmpty() ? null : options.get(0));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public static final class Provider implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.azscompanions.selection");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new CompanionSelectionMenu(id, inv);
        }
    }
}
