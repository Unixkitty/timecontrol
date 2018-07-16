package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.TimeEvents;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeHandlerClient implements ITimeHandler
{
    private static final Logger log = LogManager.getLogger(TimeHandlerClient.class.getSimpleName());

    private int debugLogDelay = 0;

    private long customtime = 0;
    private double multiplier = 0;

    @Override
    public void tick(World world)
    {
        //In system time synchronization mode we simply depend on server time packets
        if (!Config.sync_to_system())
        {
            debugLogDelay++;

            if (multiplier == 0 && debugLogDelay % 20 == 0)
            {
                log.info("Waiting for server time packet...");

                return;
            }

            customtime++;

            Numbers.setWorldtime(world, customtime, multiplier);

            if (Config.debugMode() && debugLogDelay % 20 == 0)
            {
                long worldtime = world.getWorldTime();

                log.info(String.format("Client time: %s | multiplier: %s | gamerules: %s, %s",
                        worldtime,
                        multiplier,
                        world.getGameRules().getBoolean(TimeEvents.doDaylightCycle),
                        world.getGameRules().getBoolean(TimeEvents.doDaylightCycle_tc)
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
