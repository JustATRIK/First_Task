package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import com.example.examplemod.utils.packets.EnergyAndProgressSyncPacket;
import com.example.examplemod.utils.packets.RequestToSyncPacket;
import com.example.examplemod.utils.packets.XPRemovePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;

@SideOnly(Side.CLIENT)
public class BlocksMinerGui extends GuiContainer {
    public static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("examplemod:textures/gui/blocks_destroyer.png");
    public BlocksMinerTileEntity tileEntity;
    int x, y;

    public BlocksMinerGui(Container inventorySlotsIn, BlocksMinerTileEntity tileEntity) {
        super(inventorySlotsIn);
        this.tileEntity = tileEntity;
    }

    @Override
    public void initGui() {
        ExampleMod.NETWORK.sendToServer(new RequestToSyncPacket(tileEntity.getPos()));
        x = (width - 176) / 2;
        y = (height - 166) / 2;
        addButton(new GuiButton(0, x + 122, y + 15, 18, 18, "XP"));
        super.initGui();
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        if (btn.id == 0) {
            mc.world.playSound(mc.player.posX, mc.player.posY, mc.player.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1f, 1f, false);
            ExampleMod.NETWORK.sendToServer(new XPRemovePacket(tileEntity.getPos()));
        }
        super.actionPerformed(btn);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderLabels();
        renderHoveredToolTip(mouseX, mouseY);
        renderEnergyAreaTooltips(mouseX, mouseY, x, y);
        renderProgressAreaTooltips(mouseX, mouseY, x, y);
        renderXPStoredAreaTooltips(mouseX, mouseY, x, y);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
        int i = this.guiLeft;
        int j = this.guiTop;
        drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
        drawTexturedModalRect(x + 144, y + 18, 199, 1, 5, (52 - tileEntity.getClientIntData(2) / 192));
        drawTexturedModalRect(x + 160, y + 18, 188, 1, 10, (52 - tileEntity.getClientIntData(0) / 380));
        drawTexturedModalRect(x + 152, y + 18, 182, 1, 5, (int) (52 - 52 * tileEntity.getClientFloatData(0)));
    }

    protected void renderLabels() {
        mc.fontRenderer.drawString(tileEntity.getDisplayName().getFormattedText(), 8 + x, y + 6, 10526880);
        mc.fontRenderer.drawString(mc.player.inventory.getDisplayName().getFormattedText(), 8 + x, y + 73, 10526880);
    }

    private void renderEnergyAreaTooltips(int pMouseX, int pMouseY, int x, int y) {
        if (pMouseX >= 160 + x && pMouseX <= 169 + x && pMouseY >= 18 + y && pMouseY <= 14 + y + 56) {
            FontRenderer font = mc.fontRenderer;
            net.minecraftforge.fml.client.config.GuiUtils.preItemToolTip(ItemStack.EMPTY);
            ArrayList<String> lines = new ArrayList<String>();
            lines.add(new TextComponentTranslation("examplemod.blocks_miner.gui.energy_storing").getFormattedText() + " " + tileEntity.getClientIntData(0) + "FE");
            lines.add(new TextComponentTranslation("examplemod.blocks_miner.gui.energy_consuming").getFormattedText() + " " + tileEntity.getClientIntData(1) + "FE/t");
            drawHoveringText(lines, pMouseX, pMouseY, (font == null ? fontRenderer : font));
            net.minecraftforge.fml.client.config.GuiUtils.postItemToolTip();
        }
    }

    private void renderProgressAreaTooltips(int pMouseX, int pMouseY, int x, int y) {
        if (pMouseX >= 152 + x && pMouseX <= 157 + x && pMouseY >= 18 + y && pMouseY <= 14 + y + 56) {
            FontRenderer font = mc.fontRenderer;
            net.minecraftforge.fml.client.config.GuiUtils.preItemToolTip(ItemStack.EMPTY);
            ArrayList<String> lines = new ArrayList<String>();
            lines.add(new TextComponentTranslation("examplemod.blocks_miner.gui.progress").getFormattedText() + " " + (int)(tileEntity.getClientFloatData(0) * 100) + "%");
            drawHoveringText(lines, pMouseX, pMouseY, (font == null ? fontRenderer : font));
            net.minecraftforge.fml.client.config.GuiUtils.postItemToolTip();
        }
    }

    private void renderXPStoredAreaTooltips(int pMouseX, int pMouseY, int x, int y) {
        if (pMouseX >= 144 + x && pMouseX <= 147 + x && pMouseY >= 18 + y && pMouseY <= 14 + y + 56) {
            FontRenderer font = mc.fontRenderer;
            net.minecraftforge.fml.client.config.GuiUtils.preItemToolTip(ItemStack.EMPTY);
            ArrayList<String> lines = new ArrayList<String>();
            lines.add(new TextComponentTranslation("examplemod.blocks_miner.gui.xpstored").getFormattedText() + " " + (tileEntity.getClientIntData(2)));
            drawHoveringText(lines, pMouseX, pMouseY, (font == null ? fontRenderer : font));
            net.minecraftforge.fml.client.config.GuiUtils.postItemToolTip();
        }
    }
}
