package com.example.examplemod.utils.energy;

import net.minecraftforge.energy.EnergyStorage;

public class ModEnergyStorage extends EnergyStorage {
    public ModEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return super.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return super.receiveEnergy(maxReceive, simulate);
    }

    public int setEnergy(int energy) {
        this.energy = energy;
        return energy;
    }

    public int setConsuming(int maxExtract) {
        this.maxExtract = maxExtract;
        return maxExtract;
    }

    public int getMaxExtract(){
        return this.maxExtract;
    }
}
