package com.unixkitty.timecontrol.network;

import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.network.packet.BasePacket;
import com.unixkitty.timecontrol.network.packet.ConfigS2CPacket;
import com.unixkitty.timecontrol.network.packet.GamerulesS2CPacket;
import com.unixkitty.timecontrol.network.packet.TimeS2CPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public class ModNetworkDispatcher
{
    public static final Object2ObjectOpenHashMap<Class<? extends BasePacket>, PacketDesignation> REGISTRY = new Object2ObjectOpenHashMap<>();

    private static int packetId = 0;

    static
    {
        registerPacket(TimeS2CPacket.class);
        registerPacket(GamerulesS2CPacket.class);
        registerPacket(ConfigS2CPacket.class);
    }

    private static void registerPacket(Class<? extends BasePacket> packetClass)
    {
        REGISTRY.put(packetClass, new PacketDesignation(id(), true));
    }

    private static int id()
    {
        return packetId++;
    }

    //TODO test if players in other dimensions also receive the packet
    public static void send(@NotNull ServerLevel level, @NotNull BasePacket packet)
    {
        level.players().forEach(player ->
                ServerPlayNetworking.send(player, REGISTRY.get(packet.getClass()).getId(), packet.toBytes(PacketByteBufs.create())));
    }

    public static class PacketDesignation
    {
        private final ResourceLocation id;
        private final boolean clientBound;

        private PacketDesignation(int id, boolean clientBound)
        {
            this.id = new ResourceLocation(TimeControl.MODID, "messages_" + id);
            this.clientBound = clientBound;
        }

        public ResourceLocation getId()
        {
            return this.id;
        }

        public boolean isClientBound()
        {
            return this.clientBound;
        }
    }
}
