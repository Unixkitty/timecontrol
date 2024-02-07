package com.unixkitty.timecontrol.network.packet;

import com.unixkitty.timecontrol.config.Config;
import net.minecraft.network.FriendlyByteBuf;

public class ConfigS2CPacket extends BasePacket
{
    public final int day_length_seconds;
    public final int night_length_seconds;
    public final int sync_to_system_time_rate;
    public final boolean sync_to_system_time;

    public ConfigS2CPacket()
    {
        this.day_length_seconds = Config.day_length_seconds.get();
        this.night_length_seconds = Config.night_length_seconds.get();
        this.sync_to_system_time_rate = Config.sync_to_system_time_rate.get();
        this.sync_to_system_time = Config.sync_to_system_time.get();
    }

    public ConfigS2CPacket(FriendlyByteBuf buffer)
    {
        this.day_length_seconds = buffer.readInt();
        this.night_length_seconds = buffer.readInt();
        this.sync_to_system_time_rate = buffer.readInt();
        this.sync_to_system_time = buffer.readBoolean();
    }

    @Override
    public FriendlyByteBuf toBytes(FriendlyByteBuf buffer)
    {
        buffer.writeInt(this.day_length_seconds);
        buffer.writeInt(this.night_length_seconds);
        buffer.writeInt(this.sync_to_system_time_rate);
        buffer.writeBoolean(this.sync_to_system_time);

        return buffer;
    }
}
