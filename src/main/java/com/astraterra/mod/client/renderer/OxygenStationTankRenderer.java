package com.astraterra.mod.client.renderer;

import com.astraterra.mod.block.entity.OxygenStationBlockEntity;
import com.astraterra.mod.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class OxygenStationTankRenderer implements BlockEntityRenderer<OxygenStationBlockEntity> {
    private final ItemRenderer itemRenderer;

    public OxygenStationTankRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(OxygenStationBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!blockEntity.getBlockState().is(ModBlocks.OXYGEN_STATION_TANK.get()) || blockEntity.getLevel() == null) {
            return;
        }

        OxygenStationBlockEntity station =
                OxygenStationBlockEntity.findBase(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (station == null) {
            return;
        }

        ItemStack chestplate = station.getInventory().getStackInSlot(0);
        if (chestplate.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.25D, 0.5D);
        poseStack.scale(0.65F, 0.65F, 0.65F);
        itemRenderer.renderStatic(chestplate, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, blockEntity.getLevel(), (int) blockEntity.getBlockPos().asLong());
        poseStack.popPose();
    }
}
