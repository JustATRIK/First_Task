package com.example.examplemod.utils;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public class BlocksMinerUtils {

    public static BlockPos getValidPosForSpawn(BlockPos blockPos, WorldServer world) {
        if (world.getBlockState(blockPos).getBlock() == Blocks.AIR) return blockPos;

        for (int j = -world.getWorldBorder().getSize(); j < world.getWorldBorder().getSize(); j++) {
            for (int j1 = -world.getWorldBorder().getSize(); j1 < world.getWorldBorder().getSize(); j1++) {
                BlockPos newBlockPos = new BlockPos(blockPos.getX(), 0, blockPos.getZ());
                for (int i = 0; i < 256; i++) {
                    newBlockPos.add(0, 1, 0);
                    if (world.getBlockState(blockPos).getBlock() == Blocks.AIR) return blockPos;
                }
                blockPos.add(1, 0, 0);
            }
            blockPos.add(1, 0, 1);
        }
        return blockPos;
    }
}
