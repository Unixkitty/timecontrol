package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.config.Config;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;

public abstract class TimeHandler
{
    protected long customtime;
    protected double multiplier;

    /* System time */
    private int lastMinute = 0;

    /* Arbitrary time */
    protected boolean wasDaytime = true;
    protected int dayMinutes = 0;
    protected int nightMinutes = 0;

    public static void update(TimeHandler timeHandler, @NotNull Level level, long worldtime)
    {
        timeHandler.update(level, Numbers.getCustomTime(worldtime), Numbers.getMultiplier(worldtime));

        timeHandler.dayMinutes = Config.day_length_minutes.get();
        timeHandler.nightMinutes = Config.night_length_minutes.get();
    }

    public abstract void tick(@NotNull Level level);

    public void update(@Nullable Level level, long customtime, double multiplier)
    {
        this.customtime = customtime;
        this.multiplier = multiplier;
    }

    protected void syncTimeWithSystem(@NotNull Level level)
    {
        LocalTime now = LocalTime.now();
        int minute = now.getMinute();

        if (minute != this.lastMinute)
        {
            long worldTime = level.getDayTime();
            int hour = now.getHour();
            int day = LocalDate.now().getDayOfYear();
            long time = Numbers.getSystemtimeTicks(hour, minute, day);

            this.lastMinute = minute;

            Numbers.setLevelDataWorldtime(level, time);

            if (Config.debug.get())
            {
                TimeControl.LOG.debug("System time update: {} -> {} | day {}, {}", worldTime, time, day, String.format("%02d:%02d", hour, minute));
            }
        }
    }
}
