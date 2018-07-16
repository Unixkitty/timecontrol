package com.unixkitty.timecontrol.network;

import com.unixkitty.timecontrol.TimeControl;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class MessageHandler
{
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(TimeControl.MODID + "_a");

    public static void init()
    {
        INSTANCE.registerMessage(PacketTime.Handler.class, PacketTime.class, 0, Side.CLIENT);
        INSTANCE.registerMessage(PacketGamerule.Handler.class, PacketGamerule.class, 1, Side.CLIENT);
    }
}