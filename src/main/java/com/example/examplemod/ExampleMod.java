package com.example.examplemod;

import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import com.example.examplemod.gui.ModGui;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.apache.logging.log4j.Logger;

@Mod(modid = ExampleMod.MODID, name = ExampleMod.NAME, version = ExampleMod.VERSION)
public class ExampleMod
{
    public static final String MODID = "examplemod";
    public static final String NAME = "Example Mod";
    public static final String VERSION = "1.0";
    public static final SimpleNetworkWrapper NETWORK = new SimpleNetworkWrapper("examplemodchannel");
    private static Logger logger;
    @SidedProxy(clientSide = "com.example.examplemod.ClientProxy", serverSide = "com.example.examplemod.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGui());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHarvestDrops(final BlockEvent.HarvestDropsEvent event) {
        if (event.getHarvester() == null) {
            return;
        }
        if (event.getHarvester().getGameProfile().getName().equals("examplemod.fake_player_blocks_miner")){
            World world = event.getHarvester().world;
            TileEntity tileEntity = world.getTileEntity(event.getHarvester().getPosition());
            if (tileEntity instanceof BlocksMinerTileEntity){
                for (ItemStack itemStack:event.getDrops()) {
                    ((BlocksMinerTileEntity) tileEntity).addItemStackToInventory(itemStack);
                }
            }
            event.getDrops().clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHarvestDropsExp(final BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        if (event.getPlayer().getGameProfile().getName().equals("examplemod.fake_player_blocks_miner")) {
            event.setExpToDrop(0);
        }
    }
}
