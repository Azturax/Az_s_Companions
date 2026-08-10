package com.azscompanions.menu;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class CompanionManagementMenu extends AbstractContainerMenu {
    public enum Tab {
        OVERVIEW, BODY, INVENTORY, EQUIPMENT, TASKS, HOME, PERMISSIONS, DIALOGUE, VOICE, COMPATIBILITY
    }

    private final CompanionEntity companion;
    private Tab tab = Tab.OVERVIEW;

    public CompanionManagementMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (CompanionEntity) inv.player.level().getEntity(buf.readVarInt()));
    }

    public CompanionManagementMenu(int id, Inventory inv, CompanionEntity companion) {
        super(ModMenus.COMPANION_MANAGEMENT.get(), id);
        this.companion = companion;
    }

    public CompanionEntity companion() {
        return companion;
    }

    public Tab tab() {
        return tab;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return companion != null && companion.isAlive()
                && (companion.isOwnedBy(player) || companion.isTrusted(player))
                && companion.distanceTo(player) < 8.0d;
    }

    public static final class Provider implements MenuProvider {
        private final CompanionEntity companion;

        public Provider(CompanionEntity companion) {
            this.companion = companion;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.azscompanions.management");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new CompanionManagementMenu(id, inv, companion);
        }
    }
}
