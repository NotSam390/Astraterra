package com.astraterra.mod.registry;

import com.astraterra.mod.Astraterra;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Tabs fill themselves from the item register, in registration order:
 * block items go in the Blocks tab, everything else in the Items tab.
 * New blocks/items never need to be added here by hand.
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Astraterra.MODID);

    public static final RegistryObject<CreativeModeTab> BLOCKS = CREATIVE_MODE_TABS.register("astraterra_blocks_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.OXYGEN_STATION.get()))
                    .title(Component.translatable("creativetab.astraterra.astraterra_blocks"))
                    .displayItems((parameters, output) -> {
                        for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                            if (item.get() instanceof BlockItem) output.accept(item.get());
                        }
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> ITEMS = CREATIVE_MODE_TABS.register("astraterra_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.NETHERITE_INGOT))
                    .withTabsBefore(new ResourceLocation(Astraterra.MODID, "astraterra_blocks_tab"))
                    .title(Component.translatable("creativetab.astraterra.astraterra_items"))
                    .displayItems((parameters, output) -> {
                        for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                            if (!(item.get() instanceof BlockItem)) output.accept(item.get());
                        }
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
