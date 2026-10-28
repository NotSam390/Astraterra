package com.astraterra.mod.menu;

import com.astraterra.mod.block.entity.OxygenStationBlockEntity;
import com.astraterra.mod.registry.ModBlocks;
import com.astraterra.mod.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class OxygenStationMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ContainerData stationData;

    public OxygenStationMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos(), null);
    }

    public OxygenStationMenu(int containerId, Inventory playerInventory, BlockPos stationPos,
                             OxygenStationBlockEntity station) {
        super(ModMenus.OXYGEN_STATION.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), stationPos);

        OxygenStationBlockEntity stationEntity = station;
        if (stationEntity == null) {
            BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(stationPos);
            if (!(blockEntity instanceof OxygenStationBlockEntity oxygenStation)) {
                throw new IllegalStateException("Oxygen station block entity is missing at " + stationPos);
            }
            stationEntity = oxygenStation;
        }

        addSlot(new SlotItemHandler(stationEntity.getInventory(), 0, 188, 50));
        addPlayerInventory(playerInventory);

        this.stationData = station == null ? new SimpleContainerData(3) : createStationData(stationEntity);
        addDataSlots(stationData);
    }

    private static ContainerData createStationData(OxygenStationBlockEntity station) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> station.getEnergyStored() % 30_000;
                    case 1 -> station.getEnergyStored() / 30_000;
                    case 2 -> station.getOxygenStored();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 118 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 176));
        }
    }

    public int getEnergyStored() {
        return stationData.get(0) + stationData.get(1) * 30_000;
    }

    public int getOxygenStored() {
        return stationData.get(2);
    }

    public int getEstimatedRuntimeSeconds() {
        int availableOxygen = Math.min(getOxygenStored(), getEnergyStored() / OxygenStationBlockEntity.ENERGY_PER_MB);
        int oxygenPerSecond = OxygenStationBlockEntity.OXYGEN_PER_TICK * 20;
        return (availableOxygen + oxygenPerSecond - 1) / oxygenPerSecond;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return empty;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return empty;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return empty;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.OXYGEN_STATION.get());
    }
}
