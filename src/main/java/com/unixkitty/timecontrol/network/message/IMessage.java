package com.unixkitty.timecontrol.network.message;

import net.minecraft.network.PacketBuffer;

public interface IMessage
{
    boolean isMessageInvalid();

    void encode(PacketBuffer buffer);
}
