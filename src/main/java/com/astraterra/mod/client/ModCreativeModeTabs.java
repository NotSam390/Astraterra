package com.astraterra.mod.client;

import com.astraterra.mod.Astraterra;
import com.astraterra.mod.blocks.ModBlocks;
import com.astraterra.mod.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Astraterra.MODID);

    public static final RegistryObject<CreativeModeTab> AstraTerra_Blocks = CREATIVE_MODE_TABS.register("astraterra_blocks_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.OXYGEN_STATION.get()))
                    .title(Component.translatable("creativetab.astraterra.astraterra_blocks"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.MOON_SAND.get());
                        output.accept(ModBlocks.MOON_ROCK.get());
                        output.accept(ModBlocks.CRATER_ROCK.get());
                        output.accept(ModBlocks.OXYGEN_STATION.get());
                        output.accept(ModBlocks.OXYGEN_STATION_TANK.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> AstraTerra_Items = CREATIVE_MODE_TABS.register("astraterra_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.NETHERITE_INGOT))
                    .withTabsBefore(new ResourceLocation(Astraterra.MODID, "astraterra_blocks_tab"))
                    .title(Component.translatable("creativetab.astraterra.astraterra_items"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MOON_DUST.get());
                        output.accept(ModItems.MOON_ROCK_CHUNK.get());
                        output.accept(ModItems.HELMET.get());
                        output.accept(ModItems.CHESTPLATE.get());
                        output.accept(ModItems.LEGGINGS.get());
                        output.accept(ModItems.BOOTS.get());
                    })
                    .build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
