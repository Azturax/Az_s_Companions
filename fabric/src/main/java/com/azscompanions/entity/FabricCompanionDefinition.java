package com.azscompanions.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Optional;

public record FabricCompanionDefinition(
        ResourceLocation id,
        String displayName,
        String personality,
        List<String> pronouns,
        String voiceProfile,
        ResourceLocation defaultSkin,
        String behaviorStyle,
        List<String> defaultPermissions,
        DialogueSet dialogue,
        boolean adultConfirmed
) {
    public static final Codec<FabricCompanionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(FabricCompanionDefinition::id),
            Codec.STRING.fieldOf("displayName").forGetter(FabricCompanionDefinition::displayName),
            Codec.STRING.fieldOf("personality").forGetter(FabricCompanionDefinition::personality),
            Codec.STRING.listOf().optionalFieldOf("pronouns", List.of("she", "her")).forGetter(FabricCompanionDefinition::pronouns),
            Codec.STRING.optionalFieldOf("voiceProfile", "kon_soft").forGetter(FabricCompanionDefinition::voiceProfile),
            ResourceLocation.CODEC.fieldOf("defaultSkin").forGetter(FabricCompanionDefinition::defaultSkin),
            Codec.STRING.optionalFieldOf("behaviorStyle", "gentle").forGetter(FabricCompanionDefinition::behaviorStyle),
            Codec.STRING.listOf().optionalFieldOf("defaultPermissions", List.of(
                    "follow", "gather", "farm", "build", "craft", "combat", "containers"
            )).forGetter(FabricCompanionDefinition::defaultPermissions),
            DialogueSet.CODEC.fieldOf("dialogue").forGetter(FabricCompanionDefinition::dialogue),
            Codec.BOOL.optionalFieldOf("adultConfirmed", true).forGetter(FabricCompanionDefinition::adultConfirmed)
    ).apply(instance, FabricCompanionDefinition::new));

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

        public Optional<String> pick(List<String> lines, RandomSource random) {
            if (lines == null || lines.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(lines.get(random.nextInt(lines.size())));
        }
    }
}
