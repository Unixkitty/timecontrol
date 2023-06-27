package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.network.packet.BasePacket;
import com.unixkitty.timecontrol.network.packet.ConfigS2CPacket;
import com.unixkitty.timecontrol.network.packet.GamerulesS2CPacket;
import com.unixkitty.timecontrol.network.packet.TimeS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TimeControl.MODID, value = Dist.CLIENT)
public final class ClientTimeHandler extends TimeHandler
{
    private static final TimeHandler instance = new ClientTimeHandler();
    private static final Logger log = LogManager.getLogger(ClientTimeHandler.class.getSimpleName());

    private int debugLogDelay = 0;

    private ClientTimeHandler()
    {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START && Minecraft.getInstance().level != null)
        {
            instance.tick(Minecraft.getInstance().level);
        }
    }

    public static void handlePacket(BasePacket packet)
    {
        if (packet instanceof TimeS2CPacket message)
        {
            instance.update(message.customtime, message.multiplier);
        }
        else if (packet instanceof GamerulesS2CPacket message)
        {
            ClientLevel level = Minecraft.getInstance().level;

            if (level != null)
            {
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(message.vanillaRuleValue, null);
                level.getGameRules().getRule(TimeControl.DO_DAYLIGHT_CYCLE_TC).set(message.modRuleValue, null);
            }
        }
        else if (packet instanceof ConfigS2CPacket message)
        {
            Config.day_length_minutes.set(message.day_length_minutes);
            Config.night_length_minutes.set(message.night_length_minutes);
            Config.sync_to_system_time_rate.set(message.sync_to_system_time_rate);
            Config.sync_to_system_time.set(message.sync_to_system_time);
        }
    }

    @Override
    public void tick(@Nonnull Level level)
    {
        //In system time synchronization mode we simply depend on server time packets
        if (!Config.sync_to_system_time.get() && level.dimension() == Level.OVERWORLD)
        {
            this.debugLogDelay = (this.debugLogDelay + 1) % 20;
            boolean shouldLog = this.debugLogDelay == 0 && Config.clientDebug.get();

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
}
