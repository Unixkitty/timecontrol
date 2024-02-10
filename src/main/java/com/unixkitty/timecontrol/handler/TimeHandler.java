package com.unixkitty.timecontrol.handler;

import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.config.Config;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public abstract class TimeHandler
{
    private static final Logger log = LogManager.getLogger(TimeHandler.class.getSimpleName());

    protected long customtime;
    protected double multiplier;

    /* System time */
    private int lastMinute = 0;

    /* Arbitrary time */
    protected boolean wasDaytime = true;
    protected int daySeconds = 0;
    protected int nightSeconds = 0;

    public static void update(TimeHandler timeHandler, @NotNull Level level, long worldtime)
    {
        timeHandler.update(level, Numbers.getCustomTime(worldtime), Numbers.getMultiplier(worldtime));

        timeHandler.daySeconds = Config.day_length_seconds.get();
        timeHandler.nightSeconds = Config.night_length_seconds.get();
    }

    public abstract void tick(@NotNull Level level);

    public void update(@Nullable Level level, long customtime, double multiplier)
    {
        this.customtime = customtime;
        this.multiplier = multiplier;
    }

    protected void syncTimeWithSystem(@NotNull Level level)
    {
        LocalDateTime now = LocalDateTime.now();
        int minute = now.getMinute();

        if (minute != this.lastMinute)
        {
            long worldTime = level.getDayTime();
            int hour = now.getHour();
            int day = now.getDayOfYear();

            final int _day = day;
            final int _hour = hour;
            final int _minute = minute;

            double timeOffset = Config.sync_to_system_time_offset.get();

            if (timeOffset != 0)
            {
                int offsetMinutes = (int) (timeOffset * 60);
                LocalDateTime adjustedDateTime = now.plusMinutes(offsetMinutes);

                if (adjustedDateTime.getDayOfYear() != day)
                {
                    day = adjustedDateTime.getDayOfYear();
                }

                hour = adjustedDateTime.getHour();
                minute = adjustedDateTime.getMinute();
            }

            long time = Numbers.getSystemtimeTicks(hour, minute, day);

            this.lastMinute = _minute;

            Numbers.setLevelDataWorldtime(level, time);

            if (Config.debug.get())
            {
                log.debug("System time update: {} -> {} | day {}, {} | system: day {}, {}, offset: {}",
                        worldTime, time,
                        day, String.format("%02d:%02d", hour, minute),
                        _day, String.format("%02d:%02d", _hour, _minute),
                        timeOffset);
            }
        }
    }
}
