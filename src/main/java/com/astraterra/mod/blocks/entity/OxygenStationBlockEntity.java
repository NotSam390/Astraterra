package com.astraterra.mod.blocks.entity;

import com.astraterra.mod.blocks.ModBlocks;
import com.astraterra.mod.items.armor.SpaceArmorItem;
import com.astraterra.mod.menu.OxygenStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared by both blocks of the station. The Oxygen Station (bottom) owns the data
 * (energy, oxygen tank, chestplate slot). The Oxygen Station Tank (top) only forwards the item handler.
 */
public class OxygenStationBlockEntity extends BlockEntity implements MenuProvider {
    /**
     * GregTech CEu Modern oxygen. Verify this id in-game with the fluid list / JEI.
     */
    public static final ResourceLocation OXYGEN_ID = new ResourceLocation("gtceu", "oxygen");

    public static final int ENERGY_CAPACITY = 100_000;  // FE
    public static final int MAX_RECEIVE = 2_048;        // FE per tick accepted
    public static final int TANK_CAPACITY = 16_000;     // mB
    public static final int OXYGEN_PER_TICK = 50;       // mB moved into the suit per tick
    public static final int ENERGY_PER_MB = 2;          // FE spent per mB of oxygen

    private final StationEnergy energy = new StationEnergy();

    private final FluidTank tank = new FluidTank(TANK_CAPACITY, OxygenStationBlockEntity::isOxygen) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof SpaceArmorItem armor && armor.getType() == ArmorItem.Type.CHESTPLATE;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    };

    /**
     * Pipes can fill the tank but never drain it.
     */
    private final IFluidHandler fillOnlyTank = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int t) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int t) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int t, @NotNull FluidStack stack) {
            return tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return tank.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fillOnlyTank);
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> inventory);

    public OxygenStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_STATION.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.astraterra.oxygen_station");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OxygenStationMenu(containerId, playerInventory, worldPosition, this);
    }

    public static boolean isOxygen(FluidStack stack) {
        return OXYGEN_ID.equals(ForgeRegistries.FLUIDS.getKey(stack.getFluid()));
    }

    /**
     * True if this entity belongs to the bottom Oxygen Station block.
     */
    private boolean isBase() {
        return getBlockState().is(ModBlocks.OXYGEN_STATION.get());
    }

    /**
     * The structure is complete when an Oxygen Station Tank sits directly on top of the base.
     */
    public boolean isFormed() {
        return level != null && level.getBlockState(worldPosition.above()).is(ModBlocks.OXYGEN_STATION_TANK.get());
    }

    /**
     * Given the position of a tank block, returns the base block entity below it (or null).
     */
    @Nullable
    public static OxygenStationBlockEntity findBase(BlockGetter level, BlockPos tankPos) {
        BlockPos below = tankPos.below();
        if (level.getBlockState(below).is(ModBlocks.OXYGEN_STATION.get())
                && level.getBlockEntity(below) instanceof OxygenStationBlockEntity station) {
            return station;
        }
        return null;
    }

    /* ---------------- ticking (base block only) ---------------- */

    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenStationBlockEntity be) {
        if (!be.isFormed()) {
            return;
        }
        ItemStack stack = be.inventory.getStackInSlot(0);
        if (stack.isEmpty() || !(stack.getItem() instanceof SpaceArmorItem)) {
            return;
        }

        int current = SpaceArmorItem.getOxygen(stack);
        int room = SpaceArmorItem.MAX_OXYGEN - current;
        if (room <= 0 || be.tank.isEmpty()) {
            return;
        }

        int amount = Math.min(Math.min(room, OXYGEN_PER_TICK), be.tank.getFluidAmount());
        amount = Math.min(amount, be.energy.getEnergyStored() / ENERGY_PER_MB);
        if (amount <= 0) {
            return;
        }

        FluidStack drained = be.tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }
        be.energy.consume(drained.getAmount() * ENERGY_PER_MB);
        SpaceArmorItem.setOxygen(stack, current + drained.getAmount());
        be.setChanged();
    }

    /* ---------------- accessors ---------------- */

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getOxygenStored() {
        return tank.getFluidAmount();
    }

    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, stack);
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /* ---------------- capabilities ---------------- */

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!remove) {
            if (isBase()) {
                if (cap == ForgeCapabilities.ENERGY) {
                    return energyCap.cast();
                }
                if (cap == ForgeCapabilities.FLUID_HANDLER) {
                    return fluidCap.cast();
                }
            } else if (cap == ForgeCapabilities.ITEM_HANDLER && level != null) {
                OxygenStationBlockEntity base = findBase(level, worldPosition);
                if (base != null) {
                    return base.itemCap.cast();
                }
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
        fluidCap.invalidate();
        itemCap.invalidate();
    }

    /* ---------------- saving ---------------- */

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Energy", energy.serializeNBT());
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Energy")) {
            energy.deserializeNBT(tag.get("Energy"));
        }
        if (tag.contains("Tank")) {
            tank.readFromNBT(tag.getCompound("Tank"));
        }
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    /* ---------------- energy storage ---------------- */

    private class StationEnergy extends EnergyStorage {
        StationEnergy() {
            super(ENERGY_CAPACITY, MAX_RECEIVE, 0); // receive only, never extractable by cables
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        void consume(int amount) {
            energy = Math.max(0, energy - amount);
            setChanged();
        }
    }
}