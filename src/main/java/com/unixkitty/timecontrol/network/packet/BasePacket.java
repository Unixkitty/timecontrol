package com.unixkitty.timecontrol.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public abstract class BasePacket
{
    public abstract FriendlyByteBuf toBytes(FriendlyByteBuf buffer);
}
