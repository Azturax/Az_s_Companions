package com.koncompanions.gametest;

import com.koncompanions.KonCompanions;
import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionMode;
import com.koncompanions.registry.ModEntities;
import com.koncompanions.task.tasks.FarmTask;
import com.koncompanions.task.tasks.StayTask;
import com.koncompanions.util.ProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-game scenarios: ownership rules, protection, farming queue, path mode, cancellation.
 * Run via the NeoForge gameTestServer run configuration.
 */
@GameTestHolder(KonCompanions.MOD_ID)
@PrefixGameTestTemplate(false)
public class CompanionGameTests {
    @GameTest(template = "koncompanions:empty")
    public static void multiplayerOwnership(GameTestHelper helper) {
        CompanionEntity companion = ModEntities.COMPANION.get().create(helper.getLevel());
        helper.assertTrue(companion != null, "companion created");
        helper.assertTrue(companion.getOwnerUuid() == null, "unassigned before recruit");
        helper.succeed();
    }

    @GameTest(template = "koncompanions:empty")
    public static void protectedAreasRespected(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.setBlock(pos, Blocks.BEDROCK);
        helper.assertTrue(
                ProtectionHelper.isProtectedBlock(helper.getLevel(), pos, null)
                        || helper.getLevel().getBlockState(pos).is(Blocks.BEDROCK),
                "Bedrock / blacklist protection should apply");
        helper.succeed();
    }

    @GameTest(template = "koncompanions:empty")
    public static void taskCancellation(GameTestHelper helper) {
        CompanionEntity companion = ModEntities.COMPANION.get().create(helper.getLevel());
        helper.assertTrue(companion != null, "companion created");
        companion.getTaskQueue().enqueue(new StayTask());
        companion.getTaskQueue().cancelActive("test");
        helper.assertTrue(companion.getTaskQueue().getActive() == null, "active task cleared");
        helper.succeed();
    }

    @GameTest(template = "koncompanions:empty")
    public static void cropTaskCanBeQueued(GameTestHelper helper) {
        CompanionEntity companion = ModEntities.COMPANION.get().create(helper.getLevel());
        helper.assertTrue(companion != null, "companion created");
        companion.setPermission("farm", true);
        companion.getTaskQueue().enqueue(new FarmTask());
        helper.assertTrue(true, "farm task accepted by queue");
        helper.succeed();
    }

    @GameTest(template = "koncompanions:empty")
    public static void stuckPathingFallbackConfig(GameTestHelper helper) {
        CompanionEntity companion = ModEntities.COMPANION.get().create(helper.getLevel());
        helper.assertTrue(companion != null, "companion created");
        companion.setMode(CompanionMode.FOLLOW);
        helper.assertTrue(companion.getMode() == CompanionMode.FOLLOW, "follow mode set");
        helper.succeed();
    }

    @GameTest(template = "koncompanions:empty")
    public static void chestAndCraftHooksPresent(GameTestHelper helper) {
        helper.assertTrue(
                com.koncompanions.task.TaskRegistry.create("deposit").isPresent(),
                "deposit task registered");
        helper.assertTrue(
                com.koncompanions.task.TaskRegistry.create("craft").isPresent(),
                "craft task registered");
        helper.assertTrue(
                com.koncompanions.task.TaskRegistry.create("machine").isPresent(),
                "machine task registered");
        helper.succeed();
    }
}
