package com.koncompanions.entity;

import com.koncompanions.KonCompanions;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Built-in fallbacks so first-time clients always have Kon even before datapack sync.
 */
public final class BuiltinCompanions {
    private BuiltinCompanions() {
    }

    public static void registerDefaults() {
        if (CompanionRegistry.get(CompanionRegistry.KON_ID).isPresent()) {
            return;
        }
        CompanionDefinition.DialogueSet dialogue = new CompanionDefinition.DialogueSet(
                List.of("I'm here. What should we do next?", "Hello. I'll stay close.", "Ready when you are."),
                List.of("The path ahead looks peaceful.", "Take your time. I'll watch our surroundings."),
                List.of("Working on it…", "The crops are ready to harvest."),
                List.of("Something hostile is nearby!", "Please be careful!"),
                List.of("I found some useful materials!", "Task complete."),
                List.of("I need a moment to recover."),
                List.of("I could use a snack when you have a moment."),
                List.of("My inventory is full."),
                List.of("Heading home.", "I'll wait for you at home.")
        );
        CompanionRegistry.register(new CompanionDefinition(
                CompanionRegistry.KON_ID,
                "Kon",
                "Gentle, loyal, slightly shy, practical, and encouraging. An adult fox-girl in a white shrine-maiden-inspired outfit with cyan accents, pink sash/bow, and red pom-pom shoes.",
                List.of("she", "her"),
                "kon_soft",
                ResourceLocation.fromNamespaceAndPath(KonCompanions.MOD_ID, "textures/entity/companion/kon.png"),
                ResourceLocation.fromNamespaceAndPath(KonCompanions.MOD_ID, "textures/entity/companion/kon_outfit.png"),
                "gentle",
                List.of("follow", "gather", "farm", "build", "craft", "combat", "containers"),
                dialogue,
                true
        ));
    }
}
