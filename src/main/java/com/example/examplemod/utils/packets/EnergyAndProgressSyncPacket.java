package com.example.examplemod.utils.packets;

import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import com.example.examplemod.gui.BlocksMinerGui;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class EnergyAndProgressSyncPacket implements IMessage, IMessageHandler<EnergyAndProgressSyncPacket, IMessage> {

    int x, y, z, energy, energyConsuming = 0;
    float progress;

    public EnergyAndProgressSyncPacket() {
    }

    public EnergyAndProgressSyncPacket(BlockPos blockPos, int energy, float progress, int energyConsuming) {
        x = blockPos.getX();
        y = blockPos.getY();
        z = blockPos.getZ();
        this.progress = progress;
        this.energy = energy;
        this.energyConsuming = energyConsuming;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        energy = buf.readInt();
        progress = buf.readFloat();
        energyConsuming = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(energy);
        buf.writeFloat(progress);
        buf.writeInt(energyConsuming);
    }

    @Override
    public IMessage onMessage(EnergyAndProgressSyncPacket message, MessageContext ctx) {
        BlockPos blockPos = new BlockPos(message.x, message.y, message.z);
        TileEntity tileEntity = Minecraft.getMinecraft().world.getTileEntity(blockPos);
        if (tileEntity instanceof BlocksMinerTileEntity){
            ((BlocksMinerTileEntity) tileEntity).setEnergyLevel(message.energy);
            ((BlocksMinerTileEntity) tileEntity).curBlockDamageMP = message.progress;
            ((BlocksMinerTileEntity) tileEntity).energyConsuming = message.energyConsuming;
            System.out.println(message.energy);
        }
        return null;
    }
}

