package com.astraterra.mod.blocks;

import com.astraterra.mod.blocks.entity.ModBlockEntities;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Bottom part of the oxygen station. Owns the energy buffer, oxygen tank and chestplate slot.
 * Only works while an Oxygen Station Tank block sits directly on top of it.
 */
public class OxygenStationBlock extends Block implements EntityBlock {

    public OxygenStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModBlocks.OXYGEN_STATION_TANK.get().asItem())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof OxygenStationBlockEntity station)) {
            return InteractionResult.PASS;
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

        if (!station.isFormed()) {
            player.displayClientMessage(Component.literal("Incomplete: place an Oxygen Station Tank on top."), true);
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, station, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof OxygenStationBlockEntity station) {
            station.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenStationBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.OXYGEN_STATION.get()
                ? (BlockEntityTicker<T>) (BlockEntityTicker<OxygenStationBlockEntity>) OxygenStationBlockEntity::serverTick
                : null;
    }
}