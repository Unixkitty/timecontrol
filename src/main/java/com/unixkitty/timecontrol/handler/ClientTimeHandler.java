package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.config.Config;
import com.unixkitty.timecontrol.network.packet.BasePacket;
import com.unixkitty.timecontrol.network.packet.ConfigS2CPacket;
import com.unixkitty.timecontrol.network.packet.GamerulesS2CPacket;
import com.unixkitty.timecontrol.network.packet.TimeS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public final class ClientTimeHandler extends TimeHandler
{
    private static final TimeHandler instance = new ClientTimeHandler();
    private static final Logger log = LogManager.getLogger(ClientTimeHandler.class);

    private int debugLogDelay = 0;

    private ClientTimeHandler()
    {
    }

    @Override
    public void tick(@NotNull Level level)
    {
        if (level.dimension() == Level.OVERWORLD)
        {
            //In system time synchronization mode we simply depend on server time packets
            if (Config.sync_to_system_time.get())
            {
                //If we're ignoring server on client, have to get our own system time
                if (Config.ignore_server.get() && Minecraft.getInstance().player != null && Minecraft.getInstance().player.tickCount % Config.sync_to_system_time_rate.get() == 0)
                {
                    syncTimeWithSystem(level);
                }
            }
            else
            {
                this.debugLogDelay = (this.debugLogDelay + 1) % 20;
                boolean shouldLog = this.debugLogDelay == 0 && Config.debug.get();

                if (this.multiplier == 0)
                {
                    if (Config.ignore_server.get())
                    {
                        this.multiplier = Numbers.getMultiplier(level.getDayTime());
                    }
                    else
                    {
                        if (shouldLog)
                        {
                            log.debug("Waiting for server time packet...");
                        }

                        return;
                    }
                }

                if (level.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC))
                {
                    if (Config.ignore_server.get())
                    {
                        long worldtime = level.getDayTime();

                        boolean isDaytime = Numbers.isDaytime(worldtime);

                        if (isDaytime != this.wasDaytime)
                        {
                            update(level, Numbers.getCustomTime(worldtime), Numbers.getMultiplier(worldtime));

                            this.wasDaytime = isDaytime;
                        }
                    }

                    Numbers.setWorldtime(level, ++this.customtime, this.multiplier);

                    //Detect config changes
                    if (this.daySeconds != Config.day_length_seconds.get() || this.nightSeconds != Config.night_length_seconds.get())
                    {
                        ServerTimeHandler.update(instance, level, level.getDayTime());
                    }
                }

                if (shouldLog)
                {
                    long worldtime = level.getDayTime();

                    log.debug("Client time: {} | multiplier: {} | gamerules: {}, {}",
                            worldtime,
                            this.multiplier,
                            level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT),
                            level.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC)
                    );
                }
            }
        }
    }

    public static void handlePacket(BasePacket packet, Minecraft client)
    {
        if (Config.ignore_server.get())
        {
            return;
        }

        if (packet instanceof TimeS2CPacket message)
        {
            instance.update(null, message.customtime, message.multiplier);
        }
        else if (packet instanceof GamerulesS2CPacket message)
        {
            if (client.level != null)
            {
                client.level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(message.vanillaRuleValue, null);
                client.level.getGameRules().getRule(TimeControl.DO_DAYLIGHT_CYCLE_TC).set(message.modRuleValue, null);
            }
        }
        else if (packet instanceof ConfigS2CPacket message)
        {
            Config.day_length_seconds.set(message.day_length_seconds);
            Config.night_length_seconds.set(message.night_length_seconds);
            Config.sync_to_system_time_rate.set(message.sync_to_system_time_rate);
            Config.sync_to_system_time.set(message.sync_to_system_time);

            Config.save();
        }
    }

    public static class WorldTick implements ClientTickEvents.StartTick
    {
        @Override
        public void onStartTick(Minecraft client)
        {
            if (client.level != null && !client.isPaused())
            {
                instance.tick(client.level);
            }
        }
    }
}
