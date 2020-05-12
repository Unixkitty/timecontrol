package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.events.TimeEvents;
import com.unixkitty.timecontrol.network.MessageHandler;
import com.unixkitty.timecontrol.network.message.GameruleMessageToClient;
import com.unixkitty.timecontrol.network.message.TimeMessageToClient;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;

public class ServerTimeHandler implements ITimeHandler
{
    private static final Logger log = LogManager.getLogger(ServerTimeHandler.class.getSimpleName());

//    private static final Method wakeAllPlayers = ReflectionHelper.findMethod(WorldServer.class, "wakeAllPlayers", "func_73053_d");

    /* System */
    private int lastMinute = 0;
    /* System */

    /* Arbitrary */
    private long customtime;
    private double multiplier;

    private boolean wasDaytime = true;
    /* Arbitrary */

    @Override
    public void tick(World world)
    {
        if (world.getServer() == null) return;

        if (Config.sync_to_system_time.get())
        {
            if (!world.isRemote && world.getServer().getTickCounter() % Config.sync_to_system_time_rate.get() == 0)
            {
                syncTimeWithSystem(world);
            }
        }
        else
        {
            long worldtime = world.getDayTime();

            boolean isDaytime = Numbers.isDaytime(worldtime);

            if (isDaytime != wasDaytime)
            {
                reset(worldtime);

                wasDaytime = isDaytime;
            }

            if (world.getGameRules().getBoolean(TimeEvents.DO_DAYLIGHT_CYCLE_TC))
            {
                customtime++;

                Numbers.setWorldtime(world, customtime, multiplier);
            }

            if (world.getServer().getTickCounter() % 20 == 0)
            {
                //Dummy update to detect config changes
                TimeEvents.updateServer(world.getDayTime());

                //This is to keep client multipliers in sync
                updateClients();
                GameruleMessageToClient.send((ServerWorld) world);

                if (Config.debugMode.get())
                {
                    long updatedWorldtime = world.getDayTime();

                    log.info(Numbers.progressString(updatedWorldtime, ""));

                    log.info(String.format("Server time update: %s -> %s (%s -> %s) (day %s) | multiplier: %s",
                            worldtime,
                            updatedWorldtime,
                            customtime - 1,
                            customtime,
                            Numbers.day(updatedWorldtime),
                            multiplier
                    ));
                }
            }
        }
    }

    private void reset(long worldtime)
    {
        update(Numbers.customtime(worldtime), Numbers.multiplier(worldtime));
    }

    @Override
    public void update(long customtime, double multiplier)
    {
        this.customtime = customtime;
        this.multiplier = multiplier;

        updateClients();
    }

    private void syncTimeWithSystem(World world)
    {
        //TODO different timezones for clients?
        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        if (minute != this.lastMinute)
        {
            this.lastMinute = minute;

            long worldtime = world.getDayTime();

            long time = Numbers.systemtime(hour, minute, calendar.get(Calendar.DAY_OF_YEAR));

            world.setDayTime(time);

            if (Config.debugMode.get())
            {
                log.info(String.format("System time update: %d -> %d | day %s, %s:%s", worldtime, time, calendar.get(Calendar.DAY_OF_YEAR), hour, minute));
            }
        }
    }

    private void updateClients()
    {
        MessageHandler.INSTANCE.send(
                PacketDistributor.DIMENSION.with(() -> DimensionType.OVERWORLD),
                new TimeMessageToClient(this.customtime, this.multiplier)
        );
    }
}