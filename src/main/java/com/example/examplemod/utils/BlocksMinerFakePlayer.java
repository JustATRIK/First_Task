package com.example.examplemod.utils;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.UUID;

public class BlocksMinerFakePlayer {
    public static FakePlayer initFakePlayer(final WorldServer ws, final UUID uname, final String blockName) {
        final GameProfile breakerProfile = new GameProfile(uname, "examplemod.fake_player_" + blockName);
        FakePlayer fakePlayer;
        try {
            fakePlayer = FakePlayerFactory.get(ws, breakerProfile);
        }
        catch (Exception e) {
            fakePlayer = null;
        }
        if (fakePlayer == null) return null;
        fakePlayer.connection = new NetHandlerPlayServer(FMLCommonHandler.instance().getMinecraftServerInstance(), new NetworkManager(EnumPacketDirection.SERVERBOUND), fakePlayer) {};
        return fakePlayer;
    }
}
