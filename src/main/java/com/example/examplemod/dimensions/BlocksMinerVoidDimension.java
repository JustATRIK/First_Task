package com.example.examplemod.dimensions;

import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraft.world.gen.IChunkGenerator;

public class BlocksMinerVoidDimension extends WorldProvider {

    public BlocksMinerVoidDimension() {

    }

    @Override
    public DimensionType getDimensionType() {
        return ModDimensions.BLOCKS_MINER_VOID;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorFlat(world, world.getSeed(), false, "3;minecraft:air;");
    }

    @Override
    public boolean isSurfaceWorld() {
        return false;
    }
}
