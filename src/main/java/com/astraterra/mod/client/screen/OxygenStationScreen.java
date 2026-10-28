package com.astraterra.mod.client.screen;

import com.astraterra.mod.block.entity.OxygenStationBlockEntity;
import com.astraterra.mod.menu.OxygenStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;

public class OxygenStationScreen extends AbstractContainerScreen<OxygenStationMenu> {
    private static final int PANEL = 0xFF202A35;
    private static final int EDGE = 0xFF71899A;
    private static final int ENERGY_COLOR = 0xFF55D6BE;
    private static final int OXYGEN_COLOR = 0xFF54BCE8;

    public OxygenStationScreen(OxygenStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 220;
        imageHeight = 202;
        inventoryLabelY = 107;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, PANEL);
        graphics.fill(left, top, left + imageWidth, top + 2, EDGE);
        graphics.fill(left, top + imageHeight - 2, left + imageWidth, top + imageHeight, EDGE);
        graphics.fill(left, top, left + 2, top + imageHeight, EDGE);
        graphics.fill(left + imageWidth - 2, top, left + imageWidth, top + imageHeight, EDGE);

        drawSlot(graphics, left + 187, top + 49);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, left + 7 + column * 18, top + 117 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, left + 7 + column * 18, top + 175);
        }

        drawBar(graphics, left + 8, top + 49, 160, 6,
                menu.getEnergyStored(), OxygenStationBlockEntity.ENERGY_CAPACITY, ENERGY_COLOR);
        drawBar(graphics, left + 8, top + 80, 160, 6,
                menu.getOxygenStored(), OxygenStationBlockEntity.TANK_CAPACITY, OXYGEN_COLOR);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF10161D);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF373737);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, int height,
                                int value, int capacity, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF111820);
        int filled = capacity <= 0 ? 0 : (int) ((long) width * value / capacity);
        if (filled > 0) {
            graphics.fill(x + 1, y + 1, x + filled, y + height - 1, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 7, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.astraterra.oxygen_station.energy",
                        format(menu.getEnergyStored()), format(OxygenStationBlockEntity.ENERGY_CAPACITY)),
                8, 35, ENERGY_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.astraterra.oxygen_station.oxygen",
                        format(menu.getOxygenStored()), format(OxygenStationBlockEntity.TANK_CAPACITY)),
                8, 66, OXYGEN_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.astraterra.oxygen_station.time_left",
                formatTime(menu.getEstimatedRuntimeSeconds())), 8, 91, 0xFFE5E5E5, false);
        graphics.drawString(font, Component.translatable("gui.astraterra.oxygen_station.suit_slot"),
                182, 34, 0xFFC8D2DA, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF404040, false);
    }

    private static String format(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String formatTime(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
