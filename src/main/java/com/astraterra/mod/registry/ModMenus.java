package com.astraterra.mod.registry;

import com.astraterra.mod.Astraterra;
import com.astraterra.mod.menu.OxygenStationMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Astraterra.MODID);

    public static final RegistryObject<MenuType<OxygenStationMenu>> OXYGEN_STATION =
            MENUS.register("oxygen_station", () -> IForgeMenuType.create(OxygenStationMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
