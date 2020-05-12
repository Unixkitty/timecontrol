package com.unixkitty.timecontrol.network.message;

import com.unixkitty.timecontrol.TimeControl;
import net.minecraft.network.PacketBuffer;

public class GameruleMessageToClient implements IMessage
{
    private boolean isMessageValid;

    private boolean value;

    //For use only by the message handler
    private GameruleMessageToClient()
    {
        this.isMessageValid = false;
    }

    public GameruleMessageToClient(boolean gameRuleValue)
    {
        this.value = gameRuleValue;

        this.isMessageValid = true;
    }

    public boolean get()
    {
        return this.value;
    }

    @Override
    public boolean isMessageInvalid()
    {
        return !this.isMessageValid;
    }

    public void encode(PacketBuffer buffer)
    {
        if (!isMessageValid) return;

        buffer.writeBoolean(this.value);
    }

    public static GameruleMessageToClient decode(PacketBuffer buffer)
    {
        GameruleMessageToClient message = new GameruleMessageToClient();

        try
        {
            message.value = buffer.readBoolean();
        }
        catch (IllegalArgumentException | IndexOutOfBoundsException e)
        {
            TimeControl.log().warn("Exception while reading " + GameruleMessageToClient.class.getSimpleName() + ": " + e);
            return message;
        }

        message.isMessageValid = true;

        return message;
    }

    @Override
    public String toString()
    {
        return String.format("%s[value=%s]", getClass().getSimpleName(), this.value);
    }
}
