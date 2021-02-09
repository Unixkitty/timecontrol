package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.events.TimeEvents;
import com.unixkitty.timecontrol.network.MessageHandler;
import com.unixkitty.timecontrol.network.message.GameruleMessageToClient;
import com.unixkitty.timecontrol.network.message.TimeMessageToClient;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ServerWorldInfo;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Calendar;

public class ServerTimeHandler implements ITimeHandler
{
    private static final Logger log = LogManager.getLogger(ServerTimeHandler.class.getSimpleName());

//    private static final Method wakeAllPlayers = ReflectionHelper.findMethod(WorldServer.class, "wakeAllPlayers", "func_73053_d");

    private static final Field allPlayersSleeping = ObfuscationReflectionHelper.findField(ServerWorld.class, "field_73068_P");
    private static boolean accessCheck = false;

    /* System */
    private int lastMinute = 0;
    /* System */

    /* Arbitrary */
    private long customtime;
    private double multiplier;

    private boolean wasDaytime = true;
    /* Arbitrary */

    static
    {
        allPlayersSleeping.setAccessible(true);
    }

    @Override
    public void tick(World world)
    {
        if (world.getServer() == null) return;

        ServerWorld serverWorld = (ServerWorld) world;

        if (Config.sync_to_system_time.get())
        {
            if (!serverWorld.isRemote && serverWorld.getServer().getTickCounter() % Config.sync_to_system_time_rate.get() == 0)
            {
                syncTimeWithSystem(serverWorld);
            }
        }
        else
        {
            long worldtime = serverWorld.getDayTime();

            boolean isDaytime = Numbers.isDaytime(worldtime);

            if (isDaytime != wasDaytime)
            {
                reset(worldtime);

                wasDaytime = isDaytime;
            }

            if (serverWorld.getGameRules().getBoolean(TimeEvents.DO_DAYLIGHT_CYCLE_TC))
            {
                if (areAllPlayersAsleep(serverWorld))
                {
                    long l = serverWorld.getDayTime() + 24000L;

                    ((ServerWorldInfo) serverWorld.getWorldInfo()).setDayTime(net.minecraftforge.event.ForgeEventFactory.onSleepFinished(serverWorld, l - l % 24000L, serverWorld.getDayTime()));
                }

                customtime++;

                Numbers.setWorldtime(serverWorld, customtime, multiplier);
            }

            if (serverWorld.getServer().getTickCounter() % 20 == 0)
            {
                //Dummy update to detect config changes
                TimeEvents.updateServer(serverWorld.getDayTime());

                //This is to keep client multipliers in sync
                updateClients();
                GameruleMessageToClient.send(serverWorld);

                if (Config.debugMode.get())
                {
                    long updatedWorldtime = serverWorld.getDayTime();

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

    @Override
    public void update(long customtime, double multiplier)
    {
        this.customtime = customtime;
        this.multiplier = multiplier;

        updateClients();
    }

    private boolean areAllPlayersAsleep(ServerWorld world)
    {
        try
        {
            return allPlayersSleeping.getBoolean(world) && world.getPlayers().stream().noneMatch(
                    serverPlayerEntity -> !serverPlayerEntity.isSpectator() && !serverPlayerEntity.isPlayerFullyAsleep()
            );
        }
        catch (IllegalAccessException e)
        {
            if (!accessCheck)
            {
                e.printStackTrace();

                accessCheck = true;
            }
        }

        return false;
    }

    private void reset(long worldtime)
    {
        update(Numbers.customtime(worldtime), Numbers.multiplier(worldtime));
    }

    private void syncTimeWithSystem(ServerWorld world)
    {
        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        if (minute != this.lastMinute)
        {
            long worldtime = world.getDayTime();
            long time = Numbers.systemtime(hour, minute, calendar.get(Calendar.DAY_OF_YEAR));

            this.lastMinute = minute;

            ((ServerWorldInfo) world.getWorldInfo()).setDayTime(time);

            if (Config.debugMode.get())
            {
                log.info(String.format("System time update: %d -> %d | day %s, %s:%s", worldtime, time, calendar.get(Calendar.DAY_OF_YEAR), hour, minute));
            }
        }
    }

    private void updateClients()
    {
        MessageHandler.INSTANCE.send(
                PacketDistributor.DIMENSION.with(() -> World.OVERWORLD),
                new TimeMessageToClient(this.customtime, this.multiplier)
        );
    }
}