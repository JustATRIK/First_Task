package com.example.examplemod;

import com.example.examplemod.block.ModBlocks;
import com.example.examplemod.utils.packets.EnergyAndProgressSyncPacket;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy
{
    public void preInit(FMLPreInitializationEvent event) {
        ModBlocks.register();
        ExampleMod.NETWORK.registerMessage(new EnergyAndProgressSyncPacket(), EnergyAndProgressSyncPacket.class, 0, Side.CLIENT);
    }

    public void init(FMLInitializationEvent event) {

    }

    public void postInit(FMLPostInitializationEvent event) {

    }

}
