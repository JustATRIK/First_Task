package com.example.examplemod.gui;

import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class ModGui implements IGuiHandler {

    public static final int BLOCKS_DESTROYER_GUI_ID = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == BLOCKS_DESTROYER_GUI_ID) {
            return new BlocksMinerContainer(player, (BlocksMinerTileEntity) world.getTileEntity(new BlockPos(x, y, z)));
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == BLOCKS_DESTROYER_GUI_ID) {
            return new BlocksMinerGui(new BlocksMinerContainer(player, (BlocksMinerTileEntity) world.getTileEntity(new BlockPos(x, y, z))), (BlocksMinerTileEntity) world.getTileEntity(new BlockPos(x, y, z)));
        }
        return null;
    }
}
