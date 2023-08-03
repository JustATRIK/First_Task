package com.example.examplemod.gui;

import com.example.examplemod.utils.IGuiTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class ModGui implements IGuiHandler {

    public static final int BLOCKS_DESTROYER_GUI_ID = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos blockPos = new BlockPos(x, y, z);
        TileEntity tileEntity = world.getTileEntity(blockPos);
        if (tileEntity instanceof IGuiTile) {
            return ((IGuiTile) tileEntity).createContainer(player);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos blockPos = new BlockPos(x, y, z);
        TileEntity tileEntity = world.getTileEntity(blockPos);
        if (tileEntity instanceof IGuiTile) {
            return ((IGuiTile) tileEntity).createGui(player);
        }
        return null;
    }
}
