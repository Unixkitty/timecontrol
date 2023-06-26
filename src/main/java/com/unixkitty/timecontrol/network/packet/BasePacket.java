package com.unixkitty.timecontrol.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class BasePacket
{
    public abstract void toBytes(FriendlyByteBuf buffer);

    public abstract boolean handle(Supplier<NetworkEvent.Context> contextSupplier);
}
