package com.unixkitty.timecontrol.network;

import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.network.packet.BasePacket;
import com.unixkitty.timecontrol.network.packet.GamerulesS2CPacket;
import com.unixkitty.timecontrol.network.packet.TimeS2CPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;


public class ModNetworkDispatcher
{
    public static final String PROTOCOL_VERSION = Integer.toString(2);

    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    public static void register()
    {
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(TimeControl.MODID, "messages"), () -> PROTOCOL_VERSION, ModNetworkDispatcher::shouldAcceptPacket, ModNetworkDispatcher::shouldAcceptPacket);

        registerPacket(TimeS2CPacket.class);
        registerPacket(GamerulesS2CPacket.class);
    }

    private static <T extends BasePacket> void registerPacket(Class<T> packetClass)
    {
        INSTANCE.messageBuilder(packetClass, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(buf ->
                {
                    try
                    {
                        return packetClass.getDeclaredConstructor(buf.getClass()).newInstance(buf);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException("Failed to decode packet " + packetClass.getSimpleName(), e);
                    }
                })
                .encoder(BasePacket::toBytes)
                .consumerMainThread(BasePacket::handle)
                .add();
    }

    private static boolean shouldAcceptPacket(String protocolVersion)
    {
        return PROTOCOL_VERSION.equals(protocolVersion);
    }

    public static void send(BasePacket packet, ResourceKey<Level> dimension)
    {
        INSTANCE.send(PacketDistributor.DIMENSION.with(() -> dimension), packet);
    }
}
