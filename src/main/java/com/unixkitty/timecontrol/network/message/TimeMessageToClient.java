package com.unixkitty.timecontrol.network.message;

import com.unixkitty.timecontrol.TimeControl;
import net.minecraft.network.PacketBuffer;

public class TimeMessageToClient implements IMessage
{
    private boolean isMessageValid;

    private long customtime;
    private double multiplier;

    //For use only by the message handler
    private TimeMessageToClient()
    {
        this.isMessageValid = false;
    }

    public TimeMessageToClient(long customtime, double multiplier)
    {
        this.customtime = customtime;
        this.multiplier = multiplier;

        this.isMessageValid = true;
    }

    public long getCustomtime()
    {
        return this.customtime;
    }

    public double getMultiplier()
    {
        return this.multiplier;
    }

    @Override
    public boolean isMessageInvalid()
    {
        return !this.isMessageValid;
    }

    public void encode(PacketBuffer buffer)
    {
        if (!isMessageValid) return;

        buffer.writeLong(this.customtime);
        buffer.writeDouble(this.multiplier);
    }

    public static TimeMessageToClient decode(PacketBuffer buffer)
    {
        TimeMessageToClient message = new TimeMessageToClient();

        try
        {
            message.customtime = buffer.readLong();
            message.multiplier = buffer.readDouble();
        }
        catch (IllegalArgumentException | IndexOutOfBoundsException e)
        {
            TimeControl.log().warn("Exception while reading " + TimeMessageToClient.class.getSimpleName() + ": " + e);
            return message;
        }

        message.isMessageValid = true;

        return message;
    }

    @Override
    public String toString()
    {
        return String.format("%s[customtime=%s,multiplier=%s]", getClass().getSimpleName(), this.customtime, this.multiplier);
    }
}
