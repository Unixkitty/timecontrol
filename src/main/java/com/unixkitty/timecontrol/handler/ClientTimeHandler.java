package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.events.TimeEvents;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientTimeHandler implements ITimeHandler
{
    private static final Logger log = LogManager.getLogger(ClientTimeHandler.class.getSimpleName());

    private int debugLogDelay = 0;

    private long customtime = 0;
    private double multiplier = 0;

    @Override
    public void tick(Level world)
    {
        //In system time synchronization mode we simply depend on server time packets
        if (!Config.sync_to_system_time.get())
        {
            debugLogDelay = (debugLogDelay + 1) % 20;

            if (multiplier == 0)
            {
                if (debugLogDelay == 0 && Config.debugMode.get())
                {
                    log.info("Waiting for server time packet...");
                }

                return;
            }

            if (world.getGameRules().getBoolean(TimeEvents.DO_DAYLIGHT_CYCLE_TC))
            {
                customtime++;

                Numbers.setWorldtime(world, customtime, multiplier);
            }

            if (debugLogDelay == 0 && Config.debugMode.get())
            {
                long worldtime = world.getDayTime();

                log.debug(String.format("Client time: %s | multiplier: %s | gamerules: %s, %s",
                        worldtime,
                        multiplier,
                        world.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT),
                        world.getGameRules().getBoolean(TimeEvents.DO_DAYLIGHT_CYCLE_TC)
                ));
            }
        }
    }

    @Override
    public void update(long customtime, double multiplier)
    {
        this.multiplier = multiplier;
        this.customtime = customtime;
    }
}
