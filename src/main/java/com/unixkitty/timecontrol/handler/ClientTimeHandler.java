package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.network.packet.BasePacket;
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
    private static final Logger log = LogManager.getLogger();

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
    }

    @Override
    public void tick(@Nonnull Level level)
    {
        //In system time synchronization mode we simply depend on server time packets
        if (!Config.sync_to_system_time.get() && level.dimension() == Level.OVERWORLD)
        {
            this.debugLogDelay = (this.debugLogDelay + 1) % 20;

            if (this.multiplier == 0)
            {
                if (this.debugLogDelay == 0 && Config.debugMode.get())
                {
                    log.info("Waiting for server time packet...");
                }

                return;
            }

            if (level.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC))
            {
                this.customtime++;

                Numbers.setWorldtime(level, this.customtime, this.multiplier);
            }

            if (debugLogDelay == 0 && Config.debugMode.get())
            {
                long worldtime = level.getDayTime();

                log.info(String.format("Client time: %s | multiplier: %s | gamerules: %s, %s",
                        worldtime,
                        this.multiplier,
                        level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT),
                        level.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC)
                ));
            }
        }
    }
}
