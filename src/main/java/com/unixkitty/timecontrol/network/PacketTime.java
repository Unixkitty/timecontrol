package com.unixkitty.timecontrol.network;

import com.unixkitty.timecontrol.TimeEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTime implements IMessage
{
    private long customtime;
    private double multiplier;

    /**
     * This is necessary for default instantiation
     */
    @SuppressWarnings("unused")
    public PacketTime()
    {
    }

    public PacketTime(long customtime, double multiplier)
    {
        this.customtime = customtime;
        this.multiplier = multiplier;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(this.customtime);
        buf.writeDouble(this.multiplier);
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.customtime = buf.readLong();
        this.multiplier = buf.readDouble();
    }

    public static class Handler implements IMessageHandler<PacketTime, IMessage>
    {
        @Override
        public IMessage onMessage(PacketTime message, MessageContext ctx)
        {
            Minecraft.getMinecraft().addScheduledTask(() -> TimeEvents.INSTANCE.clientUpdate(message.customtime, message.multiplier));

            return null;
        }
    }
}
