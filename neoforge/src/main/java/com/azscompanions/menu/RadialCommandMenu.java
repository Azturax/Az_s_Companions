package com.azscompanions.menu;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.network.packet.OpenCompanionCreatorPacket;
import com.azscompanions.registry.ModMenus;
import com.azscompanions.voice.DialogueCategory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Simple companion command radial — Follow / Stay / Wander / Emotes.
 * Legacy task commands are not exposed; OPEN_INVENTORY / CUSTOMIZE remain for other screens.
 */
public final class RadialCommandMenu extends AbstractContainerMenu {
    public enum Command {
        FOLLOW,
        STAY,
        WANDER,
        EMOTE_WAVE,
        EMOTE_CHEER,
        OPEN_INVENTORY,
        CUSTOMIZE
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
        if (companion.distanceTo(player) > 64.0d) {
            return;
        }
        switch (command) {
            case FOLLOW -> {
                companion.getTaskQueue().clear();
                companion.setMode(CompanionMode.FOLLOW);
            }
            case STAY -> {
                companion.getTaskQueue().clear();
                companion.setMode(CompanionMode.STAY);
            }
            case WANDER -> {
                companion.getTaskQueue().clear();
                companion.setMode(CompanionMode.WANDER);
            }
            case EMOTE_WAVE -> {
                companion.swing(InteractionHand.MAIN_HAND, true);
                companion.speak(DialogueCategory.GREETING);
            }
            case EMOTE_CHEER -> {
                companion.swing(InteractionHand.MAIN_HAND, true);
                companion.speak(DialogueCategory.SUCCESS);
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
            case OPEN_INVENTORY -> companion.openInventory(serverPlayer);
            case CUSTOMIZE -> {
                serverPlayer.closeContainer();
                PacketDistributor.sendToPlayer(serverPlayer, new OpenCompanionCreatorPacket(companion.getId()));
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

    public static final class Provider implements MenuProvider {
        private final CompanionEntity companion;

        public Provider(CompanionEntity companion) {
            this.companion = companion;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.azscompanions.radial");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new RadialCommandMenu(id, inv, companion);
        }
    }
}
