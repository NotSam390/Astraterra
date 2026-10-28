package com.astraterra.mod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.UUID;

public class SpaceArmorItem extends ArmorItem {
    // Not used yet; reserved for an attribute modifier
    private static final UUID OXYGEN_BOOST = UUID.fromString("5f8a2c1e-9b3d-4a7f-8c2e-1d6f3b5a7c9e");

    /** Oxygen capacity of the chestplate, in mB. */
    public static final int MAX_OXYGEN = 10_000;
    private static final String OXYGEN_TAG = "Oxygen";

    public SpaceArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    public static int getOxygen(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(OXYGEN_TAG);
    }

    public static void setOxygen(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt(OXYGEN_TAG, Mth.clamp(amount, 0, MAX_OXYGEN));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (getType() == ArmorItem.Type.CHESTPLATE) {
            tooltip.add(Component.literal("Oxygen: " + getOxygen(stack) + " / " + MAX_OXYGEN + " mB")
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}