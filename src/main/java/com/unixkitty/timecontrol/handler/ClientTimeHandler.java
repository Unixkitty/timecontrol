package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.events.TimeEvents;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientTimeHandler implements ITimeHandler
{
    private static final Logger log = LogManager.getLogger(ClientTimeHandler.class.getSimpleName());

    private int debugLogDelay = 0;

    private long customtime = 0;
    private double multiplier = 0;

    @Override
    public void tick(World world)
    {
        //In system time synchronization mode we simply depend on server time packets
        if (!Config.sync_to_system_time.get())
        {
            debugLogDelay++;

            if (multiplier == 0 && debugLogDelay == 20)
            {
                log.info("Waiting for server time packet...");

                return;
            }

            if (world.getGameRules().getBoolean(TimeEvents.DO_DAYLIGHT_CYCLE_TC))
            {
                customtime++;

                Numbers.setWorldtime(world, customtime, multiplier);
            }

            if (debugLogDelay == 20 && Config.debugMode.get())
            {
                long worldtime = world.getDayTime();

                log.info(String.format("Client time: %s | multiplier: %s | gamerules: %s, %s",
                        worldtime,
                        multiplier,
                        world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE),
                        world.getGameRules().getBoolean(TimeEvents.DO_DAYLIGHT_CYCLE_TC)
                ));
            }

            if (debugLogDelay >= 20)
            {
                debugLogDelay = 0;
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
