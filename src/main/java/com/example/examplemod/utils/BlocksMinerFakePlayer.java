package com.example.examplemod.utils;

import com.example.examplemod.ExampleMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class BlocksMinerFakePlayer extends FakePlayer{
    public int tileWorldID;
    public BlockPos tileBlockPos;

    public BlocksMinerFakePlayer(WorldServer world, GameProfile name, int tileWorldID, BlockPos tileBlockPos) {
        super(world, name);
        this.tileBlockPos = tileBlockPos;
        this.tileWorldID = tileWorldID;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        tileWorldID = compound.getInteger("tilesWorldID");
        tileBlockPos = new BlockPos(compound.getInteger("tileX"), compound.getInteger("tileY"), compound.getInteger("tileZ"));
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("tilesWorldID", tileWorldID);
        compound.setInteger("tileX", tileBlockPos.getX());
        compound.setInteger("tileY", tileBlockPos.getY());
        compound.setInteger("tileZ", tileBlockPos.getZ());
        return super.writeToNBT(compound);
    }

    public static WeakReference<BlocksMinerFakePlayer> initFakePlayer(final WorldServer world, final UUID uuid, final String blockName, int tilesWorldID, BlockPos blockPos) {
        WeakReference<BlocksMinerFakePlayer> fakePlayer;
        try {
            fakePlayer = new WeakReference<BlocksMinerFakePlayer>(new BlocksMinerFakePlayer(world, new GameProfile(uuid, ExampleMod.MODID + ".fake_player_" + blockName), tilesWorldID, blockPos));
        }
        catch (Exception e) {
            return null;
        }
        if (fakePlayer == null) return null;
        fakePlayer.get().connection = new NetHandlerPlayServer(FMLCommonHandler.instance().getMinecraftServerInstance(), new NetworkManager(EnumPacketDirection.SERVERBOUND), fakePlayer.get()) {};
        return fakePlayer;
    }
}
