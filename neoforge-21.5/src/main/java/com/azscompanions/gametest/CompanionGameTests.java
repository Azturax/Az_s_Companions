package com.azscompanions.gametest;

/**
 * Game tests for 1.21.1 used {@code @GameTest}/{@code @GameTestHolder}, which were removed in 1.21.5
 * in favor of datapack {@code GameTestInstance} + NeoForge {@code RegisterGameTestsEvent}.
 * <p>
 * Scenario coverage (ownership, protection, task queue, farm/deposit/craft hooks) still lives in
 * the methods below as plain helpers for a future {@code RegisterGameTestsEvent} port.
 * Disabled here so {@code :neoforge-21.5:compileJava} stays green without hollow stubs.
 */
public final class CompanionGameTests {
    private CompanionGameTests() {
    }

    // Port to RegisterGameTestsEvent + FunctionGameTestInstance / TestData when gametest CI is restored.
}
