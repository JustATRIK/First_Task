package com.example.examplemod;

import com.example.examplemod.block.ModBlocks;
import com.example.examplemod.utils.packets.EnergyAndProgressSyncPacket;
import com.example.examplemod.utils.packets.XPRemovePacket;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Method;

public class CommonProxy
{
    public static Method GET_DROP_METHOD;
    public static Method GET_SILK_TOUCH_DROP;

    public void preInit(FMLPreInitializationEvent event) {
        ModBlocks.register();
        ExampleMod.NETWORK.registerMessage(new EnergyAndProgressSyncPacket(), EnergyAndProgressSyncPacket.class, 0, Side.CLIENT);
        ExampleMod.NETWORK.registerMessage(new XPRemovePacket(), XPRemovePacket.class, 2, Side.SERVER);
    }

    public void init(FMLInitializationEvent event) {

    }

    public void postInit(FMLPostInitializationEvent event) {

    }
}
