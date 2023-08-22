package com.example.examplemod.utils.packets;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RequestToSyncPacket implements IMessage, IMessageHandler<RequestToSyncPacket, IMessage> {
    int x, y, z = 0;

    public RequestToSyncPacket() {

    }

    public RequestToSyncPacket(BlockPos pos) {
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    @Override
    public IMessage onMessage(RequestToSyncPacket message, MessageContext ctx) {
        BlockPos pos = new BlockPos(message.x, message.y, message.z);
        TileEntity tileEntity = ctx.getServerHandler().player.world.getTileEntity(pos);
        if (tileEntity instanceof BlocksMinerTileEntity){
            return new EnergyAndProgressSyncPacket(tileEntity.getPos(), ((BlocksMinerTileEntity) tileEntity).getStoredEnergy(), ((BlocksMinerTileEntity) tileEntity).curBlockDamageMP, ((BlocksMinerTileEntity) tileEntity).energyConsuming, ((BlocksMinerTileEntity) tileEntity).storedXP);
        }
        return null;
    }
}
