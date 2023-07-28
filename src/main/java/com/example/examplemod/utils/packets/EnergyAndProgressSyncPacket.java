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

    int x, y, z, energy = 0;
    float progress;

    public EnergyAndProgressSyncPacket() {
    }

    public EnergyAndProgressSyncPacket(BlockPos blockPos, int energy, float progress) {
        x = blockPos.getX();
        y = blockPos.getY();
        z = blockPos.getZ();
        this.progress = progress;
        this.energy = energy;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        energy = buf.readInt();
        progress = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(energy);
        buf.writeFloat(progress);
    }

    @Override
    public IMessage onMessage(EnergyAndProgressSyncPacket message, MessageContext ctx) {
        BlockPos blockPos = new BlockPos(message.x, message.y, message.z);
        TileEntity tileEntity = Minecraft.getMinecraft().world.getTileEntity(blockPos);
        if (tileEntity instanceof BlocksMinerTileEntity){
            ((BlocksMinerTileEntity) tileEntity).setEnergyLevel(message.energy);
            ((BlocksMinerTileEntity) tileEntity).curBlockDamageMP = message.progress;
            if (Minecraft.getMinecraft().currentScreen instanceof BlocksMinerGui && ((BlocksMinerGui)Minecraft.getMinecraft().currentScreen).tileEntity.getPos() == blockPos){
                ((BlocksMinerGui)Minecraft.getMinecraft().currentScreen).tileEntity.setEnergyLevel(message.energy);
                ((BlocksMinerGui)Minecraft.getMinecraft().currentScreen).tileEntity.curBlockDamageMP = message.progress;
            }
        }
        return null;
    }
}

