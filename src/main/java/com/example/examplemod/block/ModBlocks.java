package com.example.examplemod.block;

import com.example.examplemod.ExampleMod;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ModBlocks {

    public static final Block BLOCKS_DESTROYER_BLOCK = new BlocksMinerBlock("blocks_miner", 5.0F, 5.0F, Material.ROCK).setCreativeTab(CreativeTabs.REDSTONE);

    public static void register() {
        setBlockRegister(BLOCKS_DESTROYER_BLOCK);
        setItemBlockRegister(BLOCKS_DESTROYER_BLOCK);
        GameRegistry.registerTileEntity(((BlocksMinerBlock) BLOCKS_DESTROYER_BLOCK).getTileEntityClass(), BLOCKS_DESTROYER_BLOCK.getRegistryName().toString());
    }

    @SideOnly(Side.CLIENT)
    public static void registerRender() {
        setItemBlockRender(BLOCKS_DESTROYER_BLOCK);
    }

    private static void setBlockRegister(Block block) {
        ForgeRegistries.BLOCKS.register(block);
    }

    private static void setItemBlockRegister(Block block) {
        ForgeRegistries.ITEMS.register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    private static void setItemBlockRender(Block block) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }

}
