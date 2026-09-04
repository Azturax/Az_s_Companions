package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionEquipmentSupportTest {
    @Test
    void vanillaArmorIdsMatchSlots() {
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("minecraft:diamond_helmet", "head"));
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("minecraft:iron_chestplate", "chest"));
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("minecraft:golden_leggings", "legs"));
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("minecraft:netherite_boots", "feet"));
        assertFalse(CompanionEquipmentSupport.matchesArmorSlot("minecraft:diamond_helmet", "feet"));
    }

    @Test
    void moddedArmorWithoutVanillaClassStillMatches() {
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("draconicevolution:wyvern_helmet", "head"));
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("mod:wyvern_helm", "head"));
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("botania:manasteel_chestplate", "chest"));
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("mod:mage_robe", "chest"));
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("mod:dragon_greaves", "legs"));
        assertTrue(CompanionEquipmentSupport.looksLikeBodyArmor("minecraft:wolf_armor"));
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("mod:wolf_armor", "chest"));
    }

    @Test
    void genericArmorIdIsAllowedOnAnyHumanoidSlot() {
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("mod:wyvern_armor", "head"));
        assertTrue(CompanionEquipmentSupport.matchesArmorSlot("mod:wyvern_armor", "chest"));
        assertFalse(CompanionEquipmentSupport.matchesArmorSlot("mod:wyvern_armor", "mainhand"));
    }

    @Test
    void junkIsNotArmorOrTool() {
        assertFalse(CompanionEquipmentSupport.looksLikeArmor("minecraft:dirt"));
        assertFalse(CompanionEquipmentSupport.looksLikeArmor("minecraft:bread"));
        assertFalse(CompanionEquipmentSupport.looksLikeTool("minecraft:dirt"));
        assertFalse(CompanionEquipmentSupport.looksLikeWeapon("minecraft:apple"));
        assertFalse(CompanionEquipmentSupport.matchesArmorSlot("minecraft:dirt", "head"));
        assertFalse(CompanionEquipmentSupport.looksLikeArmor("mod:capacity_upgrade"));
    }

    @Test
    void toolsAndWeaponsIncludeModdedNames() {
        assertTrue(CompanionEquipmentSupport.looksLikeTool("draconicevolution:wyvern_pickaxe"));
        assertTrue(CompanionEquipmentSupport.looksLikeTool("mod:paxel"));
        assertTrue(CompanionEquipmentSupport.looksLikeWeapon("mod:katana"));
        assertTrue(CompanionEquipmentSupport.looksLikeWeapon("mod:energy_sword"));
        assertTrue(CompanionEquipmentSupport.looksLikeShield("mod:tower_shield"));
        assertTrue(CompanionEquipmentSupport.looksLikeToolOrWeapon("minecraft:diamond_pickaxe"));
        assertFalse(CompanionEquipmentSupport.looksLikeWeapon("minecraft:bowl"));
        assertFalse(CompanionEquipmentSupport.looksLikeTool("minecraft:diamond_sword"));
    }

    @Test
    void axeIsNotConfusedWithPickaxe() {
        assertTrue(CompanionEquipmentSupport.looksLikeTool("minecraft:iron_pickaxe"));
        assertTrue(CompanionEquipmentSupport.looksLikeTool("minecraft:iron_axe"));
        assertEquals(CompanionEquipmentSupport.ArmorKind.NONE, CompanionEquipmentSupport.armorKind("minecraft:iron_axe"));
    }
}
