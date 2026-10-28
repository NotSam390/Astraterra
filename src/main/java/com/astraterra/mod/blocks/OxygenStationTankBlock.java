package com.astraterra.mod.blocks;

import com.astraterra.mod.blocks.entity.OxygenStationBlockEntity;
import com.astraterra.mod.items.armor.SpaceArmorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * Top part of the oxygen station. Holds the space suit chestplate.
 * Has no data of its own: everything is stored in the Oxygen Station block below it.
 */
public class OxygenStationTankBlock extends Block implements EntityBlock {

    public OxygenStationTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        OxygenStationBlockEntity station = OxygenStationBlockEntity.findBase(level, pos);
        if (station == null) {
            player.displayClientMessage(Component.literal("Incomplete: place this on top of an Oxygen Station."), true);
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof SpaceArmorItem armor && armor.getType() == ArmorItem.Type.CHESTPLATE) {
            ItemStack remainder = station.getInventory().insertItem(0, held.copy(), false);
            if (remainder.getCount() < held.getCount()) {
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, remainder);
                }
                return InteractionResult.CONSUME;
            }
        }

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, station, buffer -> buffer.writeBlockPos(station.getBlockPos()));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            OxygenStationBlockEntity station = OxygenStationBlockEntity.findBase(level, pos);
            if (station != null) {
                station.dropContents(); // the chestplate can't stay inside once the tank is gone
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenStationBlockEntity(pos, state);
    }
}