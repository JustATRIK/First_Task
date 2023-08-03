package com.example.examplemod.dimensions;

import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;

public class ModDimensions {
    public static final DimensionType BLOCKS_MINER_VOID = DimensionType.register("blocks_miner_void", "_blocks_miner_void", 2, BlocksMinerVoidDimension.class, true);

    public static void registerDimensions() {
        DimensionManager.registerDimension(2, BLOCKS_MINER_VOID);
    }
}
