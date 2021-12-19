package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.events.TimeEvents;
import com.unixkitty.timecontrol.network.MessageHandler;
import com.unixkitty.timecontrol.network.message.GameruleMessageToClient;
import com.unixkitty.timecontrol.network.message.TimeMessageToClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;

public class ServerTimeHandler implements ITimeHandler
{
    private static final Logger log = LogManager.getLogger(ServerTimeHandler.class.getSimpleName());

//    private static final Field sleepStatusField = ObfuscationReflectionHelper.findField(ServerLevel.class, "f_143245_");
//    private static boolean accessCheck = false;

    /* System time */
    private int lastMinute = 0;

    /* Arbitrary time */
    private long customtime;
    private double multiplier;

    private boolean wasDaytime = true;

    /*static
    {
        sleepStatusField.setAccessible(true);
    }*/

    @Override
    public void tick(Level world)
    {
        if (world.getServer() == null) return;

        ServerLevel serverWorld = (ServerLevel) world;

        if (Config.sync_to_system_time.get())
        {
            if (!serverWorld.isClientSide && serverWorld.getServer().getTickCount() % Config.sync_to_system_time_rate.get() == 0)
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
                //TODO what about Quark's "Improved Sleeping"?
                if (areAllPlayersAsleep(serverWorld))
                {
                    serverWorld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, serverWorld.getServer());
                }

                customtime++;

                Numbers.setWorldtime(serverWorld, customtime, multiplier);
            }

            if (serverWorld.getServer().getTickCount() % 20 == 0)
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

    private boolean areAllPlayersAsleep(ServerLevel world)
    {
        final int l = world.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);

        return world.sleepStatus.areEnoughSleeping(l) && world.sleepStatus.areEnoughDeepSleeping(l, world.players());
    }

    /*private boolean areAllPlayersAsleep(ServerLevel world)
    {
        try
        {
            final int l = world.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);

            SleepStatus sleepStatus = (SleepStatus) sleepStatusField.get(world);

            return sleepStatus.areEnoughSleeping(l) && sleepStatus.areEnoughDeepSleeping(l, world.players());
        }
        catch (Exception e)
        {
            if (!accessCheck)
            {
                e.printStackTrace();

                accessCheck = true;
            }
        }

        return false;
    }*/

    private void reset(long worldtime)
    {
        update(Numbers.customtime(worldtime), Numbers.multiplier(worldtime));
    }

    private void syncTimeWithSystem(ServerLevel world)
    {
        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        if (minute != this.lastMinute)
        {
            long worldtime = world.getDayTime();
            long time = Numbers.systemtime(hour, minute, calendar.get(Calendar.DAY_OF_YEAR));

            this.lastMinute = minute;

            ((PrimaryLevelData) world.getLevelData()).setDayTime(time);

            if (Config.debugMode.get())
            {
                log.info(String.format("System time update: %d -> %d | day %s, %s:%s", worldtime, time, calendar.get(Calendar.DAY_OF_YEAR), hour, minute));
            }
        }
    }

    private void updateClients()
    {
        MessageHandler.INSTANCE.send(
                PacketDistributor.DIMENSION.with(() -> Level.OVERWORLD),
                new TimeMessageToClient(this.customtime, this.multiplier)
        );
    }
}