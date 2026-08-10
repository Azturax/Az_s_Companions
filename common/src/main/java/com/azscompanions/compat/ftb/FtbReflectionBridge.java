package com.azscompanions.compat.ftb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Reflection soft-dep on FTB Teams / Chunks / Ranks APIs (MC 1.21.x package layout).
 * Never classloads FTB types unless the corresponding mod flag was true at install time.
 */
public final class FtbReflectionBridge implements FtbCompatHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger("azscompanions/ftb");

    private final boolean teams;
    private final boolean chunks;
    private final boolean ranks;

    private Method teamsApiMethod;
    private Method teamsIsManagerLoaded;
    private Method teamsGetManager;
    private Method teamsArePlayersInSameTeam;

    private Method chunksApiMethod;
    private Method chunksIsManagerLoaded;
    private Method chunksGetManager;
    private Method chunksShouldPrevent;
    private Method chunksClaimAsPlayer;
    private Method chunksGetOrCreateDataPlayer;
    private Method chunkTeamDataUnclaim;
    private Method claimResultIsSuccess;
    private Method claimResultGetId;
    private Constructor<?> chunkPosCtor;
    private Constructor<?> chunkDimPosCtor;
    private Method playerCreateCommandSourceStack;
    private Object protectionEditBlock;
    private Object protectionInteractBlock;
    private Object interactionHandMain;
    private boolean chunksResolved;

    private Method ranksGetPermissionValue;
    private Method permAsBoolean;
    private Method permIsEmpty;
    private boolean ranksResolved;

    public FtbReflectionBridge(boolean teamsLoaded, boolean chunksLoaded, boolean ranksLoaded) {
        this.teams = teamsLoaded;
        this.chunks = chunksLoaded;
        this.ranks = ranksLoaded;
        if (teams) {
            resolveTeams();
        }
        if (chunks) {
            resolveChunks();
        }
        if (ranks) {
            resolveRanks();
        }
    }

    @Override
    public boolean teamsAvailable() {
        return teams && teamsArePlayersInSameTeam != null;
    }

    @Override
    public boolean chunksAvailable() {
        return chunks && chunksResolved && chunksShouldPrevent != null;
    }

    @Override
    public boolean ranksAvailable() {
        return ranks && ranksResolved && ranksGetPermissionValue != null;
    }

    @Override
    public boolean arePlayersInSameTeam(UUID playerA, UUID playerB) {
        if (!teamsAvailable() || playerA == null || playerB == null) {
            return false;
        }
        try {
            Object api = teamsApiMethod.invoke(null);
            if (api == null || !Boolean.TRUE.equals(teamsIsManagerLoaded.invoke(api))) {
                return false;
            }
            Object manager = teamsGetManager.invoke(api);
            if (manager == null) {
                return false;
            }
            Object result = teamsArePlayersInSameTeam.invoke(manager, playerA, playerB);
            return Boolean.TRUE.equals(result);
        } catch (Throwable t) {
            LOGGER.debug("FTB Teams same-team check failed: {}", t.toString());
            return false;
        }
    }

    @Override
    public boolean shouldPreventBlockEdit(Object level, Object blockPos, Object serverPlayer) {
        if (!chunksAvailable() || level == null || blockPos == null || serverPlayer == null) {
            return false;
        }
        try {
            Object api = chunksApiMethod.invoke(null);
            if (api == null || !Boolean.TRUE.equals(chunksIsManagerLoaded.invoke(api))) {
                return false;
            }
            Object manager = chunksGetManager.invoke(api);
            if (manager == null) {
                return false;
            }
            // Edit OR interact — mine/place/containers/use; never gates walking into the chunk.
            if (Boolean.TRUE.equals(chunksShouldPrevent.invoke(
                    manager, serverPlayer, interactionHandMain, blockPos, protectionEditBlock, null))) {
                return true;
            }
            if (protectionInteractBlock != null) {
                return Boolean.TRUE.equals(chunksShouldPrevent.invoke(
                        manager, serverPlayer, interactionHandMain, blockPos, protectionInteractBlock, null));
            }
            return false;
        } catch (Throwable t) {
            LOGGER.debug("FTB Chunks edit/interact check failed: {}", t.toString());
            return false;
        }
    }

    @Override
    public String claimChunkAsPlayer(Object serverPlayer, Object resourceKeyDimension, int chunkX, int chunkZ) {
        if (!chunksAvailable() || chunksClaimAsPlayer == null || chunkPosCtor == null || serverPlayer == null) {
            return "unavailable";
        }
        try {
            Object api = chunksApiMethod.invoke(null);
            if (api == null || !Boolean.TRUE.equals(chunksIsManagerLoaded.invoke(api))) {
                return "unavailable";
            }
            Object chunkPos = chunkPosCtor.newInstance(chunkX, chunkZ);
            Object result = chunksClaimAsPlayer.invoke(api, serverPlayer, resourceKeyDimension, chunkPos, false);
            return formatClaimResult(result);
        } catch (Throwable t) {
            LOGGER.debug("FTB claim failed: {}", t.toString());
            return "failed:exception";
        }
    }

    @Override
    public String unclaimChunkAsPlayer(Object serverPlayer, Object resourceKeyDimension, int chunkX, int chunkZ) {
        if (!chunksAvailable() || chunksGetOrCreateDataPlayer == null || chunkTeamDataUnclaim == null
                || chunkDimPosCtor == null || serverPlayer == null) {
            return "unavailable";
        }
        try {
            Object api = chunksApiMethod.invoke(null);
            if (api == null || !Boolean.TRUE.equals(chunksIsManagerLoaded.invoke(api))) {
                return "unavailable";
            }
            Object manager = chunksGetManager.invoke(api);
            if (manager == null) {
                return "unavailable";
            }
            Object teamData = chunksGetOrCreateDataPlayer.invoke(manager, serverPlayer);
            if (teamData == null) {
                return "failed:no_team_data";
            }
            Object source = playerCreateCommandSourceStack.invoke(serverPlayer);
            Object chunkDimPos = chunkDimPosCtor.newInstance(resourceKeyDimension, chunkX, chunkZ);
            // adminOverride=false — cannot steal others' claims
            Object result = chunkTeamDataUnclaim.invoke(teamData, source, chunkDimPos, false, false);
            return formatClaimResult(result);
        } catch (Throwable t) {
            LOGGER.debug("FTB unclaim failed: {}", t.toString());
            return "failed:exception";
        }
    }

    @Override
    public boolean hasPermission(Object serverPlayer, String node, boolean defaultIfMissing) {
        if (!ranksAvailable() || serverPlayer == null || node == null || node.isBlank()) {
            return defaultIfMissing;
        }
        try {
            Object value = ranksGetPermissionValue.invoke(null, serverPlayer, node);
            if (value == null || Boolean.TRUE.equals(permIsEmpty.invoke(value))) {
                return defaultIfMissing;
            }
            @SuppressWarnings("unchecked")
            Optional<Boolean> asBool = (Optional<Boolean>) permAsBoolean.invoke(value);
            return asBool != null && asBool.isPresent() ? asBool.get() : defaultIfMissing;
        } catch (Throwable t) {
            LOGGER.debug("FTB Ranks check failed for {}: {}", node, t.toString());
            return defaultIfMissing;
        }
    }

    private String formatClaimResult(Object result) throws Exception {
        if (result == null) {
            return "failed:null";
        }
        if (claimResultIsSuccess != null && Boolean.TRUE.equals(claimResultIsSuccess.invoke(result))) {
            return "ok";
        }
        if (claimResultGetId != null) {
            Object id = claimResultGetId.invoke(result);
            return "failed:" + (id == null ? "unknown" : id.toString());
        }
        return "failed:" + result;
    }

    private void resolveTeams() {
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            teamsApiMethod = apiClass.getMethod("api");
            Class<?> apiIface = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            teamsIsManagerLoaded = apiIface.getMethod("isManagerLoaded");
            teamsGetManager = apiIface.getMethod("getManager");
            Class<?> managerClass = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager");
            teamsArePlayersInSameTeam = managerClass.getMethod("arePlayersInSameTeam", UUID.class, UUID.class);
            LOGGER.info("FTB Teams reflection bridge ready");
        } catch (Throwable t) {
            LOGGER.warn("FTB Teams present but API reflect failed: {}", t.toString());
            teamsApiMethod = null;
            teamsArePlayersInSameTeam = null;
        }
    }

    private void resolveChunks() {
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            chunksApiMethod = apiClass.getMethod("api");
            Class<?> apiIface = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
            chunksIsManagerLoaded = apiIface.getMethod("isManagerLoaded");
            chunksGetManager = apiIface.getMethod("getManager");
            Class<?> managerClass = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
            Class<?> protectionClass = Class.forName("dev.ftb.mods.ftbchunks.api.Protection");
            Class<?> handClass = Class.forName("net.minecraft.world.InteractionHand");
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            Class<?> chunkPosClass = Class.forName("net.minecraft.world.level.ChunkPos");
            Class<?> resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
            Class<?> claimResultClass = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimResult");
            Class<?> chunkTeamDataClass = Class.forName("dev.ftb.mods.ftbchunks.api.ChunkTeamData");
            Class<?> chunkDimPosClass = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
            Class<?> commandSourceStackClass = Class.forName("net.minecraft.commands.CommandSourceStack");

            chunksShouldPrevent = managerClass.getMethod(
                    "shouldPreventInteraction",
                    entityClass, handClass, blockPosClass, protectionClass, entityClass);
            chunksClaimAsPlayer = apiIface.getMethod(
                    "claimAsPlayer", serverPlayerClass, resourceKeyClass, chunkPosClass, boolean.class);
            chunksGetOrCreateDataPlayer = managerClass.getMethod("getOrCreateData", serverPlayerClass);
            chunkTeamDataUnclaim = chunkTeamDataClass.getMethod(
                    "unclaim", commandSourceStackClass, chunkDimPosClass, boolean.class, boolean.class);
            claimResultIsSuccess = claimResultClass.getMethod("isSuccess");
            claimResultGetId = claimResultClass.getMethod("getResultId");
            chunkPosCtor = chunkPosClass.getConstructor(int.class, int.class);
            chunkDimPosCtor = chunkDimPosClass.getConstructor(resourceKeyClass, int.class, int.class);
            playerCreateCommandSourceStack = serverPlayerClass.getMethod("createCommandSourceStack");

            Field edit = protectionClass.getField("EDIT_BLOCK");
            protectionEditBlock = edit.get(null);
            try {
                Field interact = protectionClass.getField("INTERACT_BLOCK");
                protectionInteractBlock = interact.get(null);
            } catch (NoSuchFieldException ignored) {
                protectionInteractBlock = null;
            }
            interactionHandMain = Enum.valueOf(handClass.asSubclass(Enum.class), "MAIN_HAND");
            chunksResolved = true;
            LOGGER.info("FTB Chunks reflection bridge ready (claim + protect)");
        } catch (Throwable t) {
            LOGGER.warn("FTB Chunks present but API reflect failed: {}", t.toString());
            chunksResolved = false;
            chunksShouldPrevent = null;
        }
    }

    private void resolveRanks() {
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            Class<?> playerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            ranksGetPermissionValue = apiClass.getMethod("getPermissionValue", playerClass, String.class);
            Class<?> permValue = Class.forName("dev.ftb.mods.ftbranks.api.PermissionValue");
            permAsBoolean = permValue.getMethod("asBoolean");
            permIsEmpty = permValue.getMethod("isEmpty");
            ranksResolved = true;
            LOGGER.info("FTB Ranks reflection bridge ready");
        } catch (Throwable t) {
            LOGGER.warn("FTB Ranks present but API reflect failed: {}", t.toString());
            ranksResolved = false;
            ranksGetPermissionValue = null;
        }
    }
}
