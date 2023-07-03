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
        //In system time synchronization mode we simply depend on server time packets
        if (!Config.sync_to_system_time.get() && level.dimension() == Level.OVERWORLD)
        {
            this.debugLogDelay = (this.debugLogDelay + 1) % 20;
            boolean shouldLog = this.debugLogDelay == 0 && Config.debug.get();

            if (this.multiplier == 0)
            {
                if (shouldLog)
                {
                    log.debug("Waiting for server time packet...");
                }

                return;
            }

            if (level.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC))
            {
                Numbers.setWorldtime(level, ++this.customtime, this.multiplier);
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

    public static void handlePacket(BasePacket packet, Minecraft client)
    {
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
            Config.day_length_minutes.set(message.day_length_minutes);
            Config.night_length_minutes.set(message.night_length_minutes);
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
            if (client.level != null)
            {
                instance.tick(client.level);
            }
        }
    }
}
