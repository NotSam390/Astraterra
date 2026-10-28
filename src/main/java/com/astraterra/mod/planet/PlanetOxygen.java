package com.astraterra.mod.planet;

import com.astraterra.mod.Astraterra;
import com.astraterra.mod.item.SpaceArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * On planets where {@link Planet#breathable()} is false, players breathe the
 * oxygen stored in their space suit chestplate (filled by the Oxygen Station).
 * No full suit, or an empty chestplate = suffocation damage.
 *
 * Delete this class if you'd rather handle breathing another way.
 */
@Mod.EventBusSubscriber(modid = Astraterra.MODID)
public class PlanetOxygen {

    /** mB used per tick. SpaceArmorItem.MAX_OXYGEN (10 000) / 1 = about 8 minutes. */
    public static final int OXYGEN_PER_TICK = 1;

    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final float DAMAGE = 2.0F;

    private static final EquipmentSlot[] SUIT_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;

        Player player = event.player;
        if (player.isCreative() || player.isSpectator()) return;

        Planet planet = Planets.get(player.level());
        if (planet == null || planet.breathable()) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (hasFullSuit(player) && SpaceArmorItem.getOxygen(chest) > 0) {
            SpaceArmorItem.setOxygen(chest, SpaceArmorItem.getOxygen(chest) - OXYGEN_PER_TICK);
            return;
        }

        if (player.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
            player.hurt(player.damageSources().drown(), DAMAGE);
            player.displayClientMessage(Component.translatable("message.astraterra.no_oxygen")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    private static boolean hasFullSuit(Player player) {
        for (EquipmentSlot slot : SUIT_SLOTS) {
            if (!(player.getItemBySlot(slot).getItem() instanceof SpaceArmorItem)) return false;
        }
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof SpaceArmorItem armor
                && armor.getType() == ArmorItem.Type.CHESTPLATE;
    }
}
