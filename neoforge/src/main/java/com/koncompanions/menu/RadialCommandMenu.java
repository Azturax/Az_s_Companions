package com.koncompanions.menu;

import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionMode;
import com.koncompanions.network.packet.OpenCompanionCreatorPacket;
import com.koncompanions.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Legacy radial menu (interact entry removed). Non-follow commands are no-ops. */
public final class RadialCommandMenu extends AbstractContainerMenu {
    public enum Command {
        FOLLOW, STAY, GUARD, GATHER, FARM, BUILD, CRAFT, DEPOSIT, SLEEP, RETURN_HOME, STOP_TASK, OPEN_INVENTORY, CUSTOMIZE
    }

    private final CompanionEntity companion;

    public RadialCommandMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (CompanionEntity) inv.player.level().getEntity(buf.readVarInt()));
    }

    public RadialCommandMenu(int id, Inventory inv, CompanionEntity companion) {
        super(ModMenus.RADIAL_COMMAND.get(), id);
        this.companion = companion;
    }

    public CompanionEntity companion() {
        return companion;
    }

    public void runCommand(Player player, Command command) {
        if (companion == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!companion.isOwnedBy(player) && !companion.isTrusted(player)) {
            return;
        }
        switch (command) {
            case CUSTOMIZE -> {
                serverPlayer.closeContainer();
                PacketDistributor.sendToPlayer(serverPlayer, new OpenCompanionCreatorPacket(companion.getId()));
            }
            default -> {
                companion.setMode(CompanionMode.FOLLOW);
                companion.getTaskQueue().clear();
            }
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

    public static final class Provider implements MenuProvider {
        private final CompanionEntity companion;

        public Provider(CompanionEntity companion) {
            this.companion = companion;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.koncompanions.radial");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new RadialCommandMenu(id, inv, companion);
        }
    }
}
