package com.astraterra.mod.items;

import com.astraterra.mod.Astraterra;
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
