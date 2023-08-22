package com.example.examplemod.utils.packets;

import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class XPRemovePacket implements IMessage, IMessageHandler<XPRemovePacket, IMessage> {
    int x, y, z;

    public XPRemovePacket() {

    }

    public XPRemovePacket(BlockPos pos) {
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
    public IMessage onMessage(XPRemovePacket message, MessageContext ctx) {
        BlockPos blockPos = new BlockPos(message.x, message.y, message.z);
        TileEntity tileEntity = ctx.getServerHandler().player.world.getTileEntity(blockPos);
        if (tileEntity instanceof BlocksMinerTileEntity) {
            ctx.getServerHandler().player.addExperience(((BlocksMinerTileEntity) tileEntity).storedXP);
            ((BlocksMinerTileEntity) tileEntity).storedXP = 0;
        }
        return null;
    }
}
