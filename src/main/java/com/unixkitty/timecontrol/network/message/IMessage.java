package com.unixkitty.timecontrol.network.message;

import net.minecraft.network.FriendlyByteBuf;

public interface IMessage
{
    boolean isMessageInvalid();

    void encode(FriendlyByteBuf buffer);
}
