package com.unixkitty.timecontrol.network;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.TimeEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.logging.log4j.LogManager;

public class PacketGamerule implements IMessage
{
    private boolean doDaylightCycle_tc;

    /**
     * This is necessary for default instantiation
     */
    @SuppressWarnings("unused")
    public PacketGamerule()
    {
    }

    public PacketGamerule(boolean doDaylightCycle_tc)
    {
        this.doDaylightCycle_tc = doDaylightCycle_tc;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeBoolean(this.doDaylightCycle_tc);
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.doDaylightCycle_tc = buf.readBoolean();
    }

    public static class Handler implements IMessageHandler<PacketGamerule, IMessage>
    {
        @Override
        public IMessage onMessage(PacketGamerule message, MessageContext ctx)
        {
            Minecraft.getMinecraft().addScheduledTask(() ->
            {
                //if (Minecraft.getMinecraft().world.provider.getDimension() == 0)
                //{
                Minecraft.getMinecraft().world.getGameRules().setOrCreateGameRule(TimeEvents.doDaylightCycle_tc, Boolean.toString(message.doDaylightCycle_tc));

                if (Config.debugMode())
                {
                    LogManager.getLogger().info("Network packet for gamerule " + TimeEvents.doDaylightCycle_tc + " received, value: " + message.doDaylightCycle_tc);
                }
                //}
            });

            return null;
        }
    }
}
