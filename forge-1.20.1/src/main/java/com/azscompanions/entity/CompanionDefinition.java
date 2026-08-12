package com.azscompanions.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Data-driven companion profile loaded from datapacks / resource JSON.
 * Characters are always treated as adult; age is not a configurable field.
 */
public record CompanionDefinition(
        ResourceLocation id,
        String displayName,
        String personality,
        List<String> pronouns,
        String voiceProfile,
        ResourceLocation defaultSkin,
        ResourceLocation defaultOutfit,
        String behaviorStyle,
        List<String> defaultPermissions,
        DialogueSet dialogue,
        boolean adultConfirmed
) {
    public static final Codec<CompanionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(CompanionDefinition::id),
            Codec.STRING.fieldOf("displayName").forGetter(CompanionDefinition::displayName),
            Codec.STRING.fieldOf("personality").forGetter(CompanionDefinition::personality),
            Codec.STRING.listOf().optionalFieldOf("pronouns", List.of("she", "her")).forGetter(CompanionDefinition::pronouns),
            Codec.STRING.optionalFieldOf("voiceProfile", "kon_soft").forGetter(CompanionDefinition::voiceProfile),
            ResourceLocation.CODEC.fieldOf("defaultSkin").forGetter(CompanionDefinition::defaultSkin),
            ResourceLocation.CODEC.optionalFieldOf("defaultOutfit",
                    new ResourceLocation("azscompanions", "textures/entity/companion/kon_outfit.png"))
                    .forGetter(CompanionDefinition::defaultOutfit),
            Codec.STRING.optionalFieldOf("behaviorStyle", "gentle").forGetter(CompanionDefinition::behaviorStyle),
            Codec.STRING.listOf().optionalFieldOf("defaultPermissions", List.of(
                    "follow", "gather", "farm", "build", "craft", "combat", "containers"
            )).forGetter(CompanionDefinition::defaultPermissions),
            DialogueSet.CODEC.fieldOf("dialogue").forGetter(CompanionDefinition::dialogue),
            Codec.BOOL.optionalFieldOf("adultConfirmed", true).forGetter(CompanionDefinition::adultConfirmed)
    ).apply(instance, CompanionDefinition::new));

    public record DialogueSet(
            List<String> greetings,
            List<String> idle,
            List<String> taskProgress,
            List<String> danger,
            List<String> success,
            List<String> lowHealth,
            List<String> hunger,
            List<String> inventoryFull,
            List<String> returnHome
    ) {
        public static final Codec<DialogueSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("greetings").forGetter(DialogueSet::greetings),
                Codec.STRING.listOf().fieldOf("idle").forGetter(DialogueSet::idle),
                Codec.STRING.listOf().fieldOf("taskProgress").forGetter(DialogueSet::taskProgress),
                Codec.STRING.listOf().fieldOf("danger").forGetter(DialogueSet::danger),
                Codec.STRING.listOf().fieldOf("success").forGetter(DialogueSet::success),
                Codec.STRING.listOf().fieldOf("lowHealth").forGetter(DialogueSet::lowHealth),
                Codec.STRING.listOf().fieldOf("hunger").forGetter(DialogueSet::hunger),
                Codec.STRING.listOf().fieldOf("inventoryFull").forGetter(DialogueSet::inventoryFull),
                Codec.STRING.listOf().fieldOf("returnHome").forGetter(DialogueSet::returnHome)
        ).apply(instance, DialogueSet::new));

        public Optional<String> pick(List<String> lines, net.minecraft.util.RandomSource random) {
            if (lines == null || lines.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(lines.get(random.nextInt(lines.size())));
        }
    }
}
