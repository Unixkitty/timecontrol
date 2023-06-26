package com.unixkitty.timecontrol.network.packet;

import com.unixkitty.timecontrol.handler.ClientTimeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TimeS2CPacket extends BasePacket
{
    public final long customtime;
    public final double multiplier;

    public TimeS2CPacket(long customtime, double multiplier)
    {
        this.customtime = customtime;
        this.multiplier = multiplier;
    }

    public TimeS2CPacket(FriendlyByteBuf buffer)
    {
        this.customtime = buffer.readLong();
        this.multiplier = buffer.readDouble();
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer)
    {
        buffer.writeLong(this.customtime);
        buffer.writeDouble(this.multiplier);
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> ClientTimeHandler.handlePacket(this));

        context.setPacketHandled(true);

        return true;
    }
}
