package com.unixkitty.timecontrol.network.packet;

import net.minecraft.network.FriendlyByteBuf;

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
    public FriendlyByteBuf toBytes(FriendlyByteBuf buffer)
    {
        buffer.writeLong(this.customtime);
        buffer.writeDouble(this.multiplier);

        return buffer;
    }
}
