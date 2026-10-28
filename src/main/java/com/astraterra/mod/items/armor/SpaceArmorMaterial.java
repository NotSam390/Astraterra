package com.astraterra.mod.items.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public enum SpaceArmorMaterial implements ArmorMaterial {
    INSTANCE;

    // Change to your actual mod id (must match the texture folder: assets/<modid>/textures/models/armor/space_suit_layer_1.png)
    public static final String MOD_ID = "astraterra";
    public static final String NAME = "space_suit";

    public static final int DURABILITY = 333;
    public static final float TOUGHNESS = 15.0f;
    public static final float KNOCKBACK_RESISTANCE = 0.0f;
    public static final int ENCHANTMENT_VALUE = 30;

    // Base durability multipliers per slot (same ratios as vanilla): boots, leggings, chestplate, helmet
    private static final int[] DURABILITY_MULTIPLIER = {13, 15, 16, 11};
    // Defense per slot: boots, leggings, chestplate, helmet
    private static final int[] DEFENSE = {3, 6, 8, 3};

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY * DURABILITY_MULTIPLIER[type.ordinal()] / 13;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return DEFENSE[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return MOD_ID + ":" + NAME;
    }

    @Override
    public float getToughness() {
        return TOUGHNESS;
    }

    @Override
    public float getKnockbackResistance() {
        return KNOCKBACK_RESISTANCE;
    }
}