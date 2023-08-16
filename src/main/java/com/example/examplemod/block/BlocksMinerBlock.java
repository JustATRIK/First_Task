package com.example.examplemod.block;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import com.example.examplemod.gui.ModGui;
import com.example.examplemod.utils.BlocksMinerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlocksMinerBlock extends BlockContainer {
    public static final PropertyDirection FACING = BlockDirectional.FACING;
    public BlocksMinerBlock(String name, float hardness, float resistance, Material blockMaterialIn) {
        super(blockMaterialIn);
        this.setRegistryName(name);
        this.setTranslationKey("examplemod.blocks_miner");
        this.setHardness(hardness);
        this.setResistance(resistance);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }
    @Override
    public boolean hasTileEntity()
    {
        return true;
    }
    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new BlocksMinerTileEntity();
    }
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        playerIn.openGui(ExampleMod.MODID, ModGui.BLOCKS_DESTROYER_GUI_ID, worldIn,pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
    public Class<? extends TileEntity> getTileEntityClass() {
        return BlocksMinerTileEntity.class;
    }
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        BlocksMinerTileEntity tileEntity = (BlocksMinerTileEntity) worldIn.getTileEntity(pos);
        if (tileEntity.fakePlayer != null && tileEntity.fakePlayer.get() != null) BlocksMinerUtils.blockedPositions.remove(tileEntity.fakePlayer.get().getPosition());
        super.breakBlock(worldIn, pos, state);
        for (ItemStack itemStack:tileEntity.items) {
            Block.spawnAsEntity(worldIn, pos, itemStack);
        }
    }
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer));
    }
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7));
    }
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(FACING).getIndex();
        return i;
    }
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[] {FACING});
    }
}
