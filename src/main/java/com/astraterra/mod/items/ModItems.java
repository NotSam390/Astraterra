package com.astraterra.mod.items;

import com.astraterra.mod.Astraterra;
import com.astraterra.mod.items.armor.SpaceArmorItem;
import com.astraterra.mod.items.armor.SpaceArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Astraterra.MODID);

    public static final RegistryObject<Item> MOON_DUST = ITEMS.register("moon_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOON_ROCK_CHUNK = ITEMS.register("moon_rock_chunk",
            () -> new Item(new Item.Properties()));

    // Space suit armor pieces
    public static final RegistryObject<SpaceArmorItem> HELMET = ITEMS.register("space_helmet",
            () -> new SpaceArmorItem(SpaceArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<SpaceArmorItem> CHESTPLATE = ITEMS.register("space_chestplate",
            () -> new SpaceArmorItem(SpaceArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<SpaceArmorItem> LEGGINGS = ITEMS.register("space_leggings",
            () -> new SpaceArmorItem(SpaceArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<SpaceArmorItem> BOOTS = ITEMS.register("space_boots",
            () -> new SpaceArmorItem(SpaceArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}