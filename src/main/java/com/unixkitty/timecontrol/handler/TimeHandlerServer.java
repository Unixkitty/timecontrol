package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.network.MessageHandler;
import com.unixkitty.timecontrol.network.PacketTime;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

public class TimeHandlerServer implements ITimeHandler
{
    private static final Logger log = LogManager.getLogger(TimeHandlerServer.class.getSimpleName());

    private static final Method wakeAllPlayers = ReflectionHelper.findMethod(WorldServer.class, "wakeAllPlayers", "func_73053_d");

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
        if (Config.sync_to_system())
        {
            if (!world.isRemote && world.getMinecraftServer().getTickCounter() % Config.sync_to_system_rate() == 0)
            {
                syncTimeWithSystem(world);
            }
        }
        else
        {
            long worldtime = world.getWorldTime();

            boolean isDaytime = Numbers.isDaytime(worldtime);

            if (isDaytime != wasDaytime)
            {
                reset(worldtime);

                wasDaytime = isDaytime;
            }

            try
            {
                if (world instanceof WorldServer && ((WorldServer) world).areAllPlayersAsleep())
                {
                    long i = worldtime + 24000L;

                    i = i - i % 24000L;

                    world.provider.setWorldTime(i);

                    reset(i);

                    wasDaytime = true;

                    wakeAllPlayers.invoke(world);
                }
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                log.error("Unable to wake players!", e);
            }

            customtime++;

            Numbers.setWorldtime(world, customtime, multiplier);

            if (world.getMinecraftServer().getTickCounter() % 20 == 0)
            {
                //This is to keep client multipliers in sync
                MessageHandler.INSTANCE.sendToAll(new PacketTime(customtime, multiplier));

                if (Config.debugMode())
                {
                    long updatedWorldtime = world.getWorldTime();

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
        MessageHandler.INSTANCE.sendToAll(new PacketTime(customtime, multiplier));

        this.customtime = customtime;
        this.multiplier = multiplier;
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

            long worldtime = world.getWorldTime();

            long time = Numbers.systemtime(hour, minute, calendar.get(Calendar.DAY_OF_YEAR));

            world.provider.setWorldTime(time);

            if (Config.debugMode())
            {
                log.info(String.format("System time update: %d -> %d | day %s, %s:%s", worldtime, time, calendar.get(Calendar.DAY_OF_YEAR), hour, minute));
            }
        }
    }
}