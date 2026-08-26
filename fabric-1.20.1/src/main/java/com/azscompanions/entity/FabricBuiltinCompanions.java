package com.azscompanions.entity;

import com.azscompanions.AzsCompanionsFabric;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class FabricBuiltinCompanions {
    private FabricBuiltinCompanions() {
    }

    public static void registerDefaults() {
        if (FabricCompanionRegistry.get(FabricCompanionRegistry.KON_ID).isPresent()) {
            return;
        }
        FabricCompanionDefinition.DialogueSet dialogue = new FabricCompanionDefinition.DialogueSet(
                List.of("I'm here. What should we do next?", "Hello. I'll stay close.", "Ready when you are."),
                List.of("The path ahead looks peaceful.", "Take your time. I'll watch our surroundings.",
                        "The wind shifted. Smells like rain.", "Quiet stretch. That's fine with me.",
                        "That hill looks promising."),
                List.of("Working on it…", "The crops are ready to harvest."),
                List.of("Something hostile is nearby!", "Please be careful!"),
                List.of("I found some useful materials!", "Task complete."),
                List.of("I need a moment to recover."),
                List.of("I could use a snack when you have a moment."),
                List.of("My inventory is full."),
                List.of("Heading home.", "I'll wait for you at home.")
        );
        FabricCompanionRegistry.register(new FabricCompanionDefinition(
                FabricCompanionRegistry.KON_ID,
                "Kon",
                "Gentle, loyal, slightly shy, practical, and encouraging. An adult fox-girl in a white shrine-maiden-inspired outfit with cyan accents, pink sash/bow, and red pom-pom shoes.",
                List.of("she", "her"),
                "kon_soft",
                new ResourceLocation(AzsCompanionsFabric.MOD_ID, "textures/entity/companion/kon.png"),
                "gentle",
                List.of("follow", "gather", "farm", "build", "craft", "combat", "containers"),
                dialogue,
                true
        ));
    }
}
