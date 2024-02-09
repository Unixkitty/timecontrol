package com.unixkitty.timecontrol;

import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.PrimaryLevelData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Numbers
{
    public static final long DAY_TICKS = 24000L;
    public static final long HALF_DAY_TICKS = DAY_TICKS / 2; //This also corresponds to when nighttime starts in vanilla
    public static final double MAX_TIME_SHIFT = 23.0D;
    public static final List<Double> TIME_SHIFT_LIST;

    private static final int real_life_hour_offset = 6;
    private static final int hours_per_day = 24;
    private static final int ticks_per_hour = (int) (DAY_TICKS / hours_per_day);
    private static final int real_life_second_is_ticks = 20;
    private static final int real_life_minute_is_ticks = real_life_second_is_ticks * 60;
    private static final double vanilla_multiplier = HALF_DAY_TICKS / (double) real_life_second_is_ticks;
    private static final double real_life_minute_multiplier = real_life_minute_is_ticks / 72.0D; //Minecraft "time" is 72 times faster than IRL: 1440 IRL minutes / 20 (length of a Mineraft day in minutes)

    static
    {
        List<Double> tempList = new ArrayList<>();

        for (double i = -MAX_TIME_SHIFT; i <= MAX_TIME_SHIFT; i += 0.5D)
        {
            tempList.add(i);
        }

        TIME_SHIFT_LIST = new DoubleImmutableList(tempList);
    }

    public static double getMultiplier(long worldtime)
    {
        return getMultiplier(isDaytime(worldtime));
    }

    public static long getCustomTime(long worldtime)
    {
        return (long) (worldtime * getMultiplier(worldtime));
    }

    public static void setWorldtime(Level level, long customtime, double multiplier)
    {
        setLevelDataWorldtime(level, getWorldtime(customtime, multiplier));
    }

    public static void setLevelDataWorldtime(Level level, long worldtime)
    {
        LevelData worldInfo = level.getLevelData();

        if (level.isClientSide && worldInfo instanceof ClientLevel.ClientLevelData)
        {
            ((ClientLevel.ClientLevelData) worldInfo).setDayTime(worldtime);
        }
        else if (worldInfo instanceof PrimaryLevelData)
        {
            ((PrimaryLevelData) worldInfo).setDayTime(worldtime);
        }
    }

    public static String getProgressString(long item)
    {
        final int stringLength = 50;
        item = item % HALF_DAY_TICKS;
        int percent = (int) (item * 100 / HALF_DAY_TICKS);
        int division = 100 / stringLength;

        return String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")) +
                String.format(" %d%% [", percent) +
                String.join("", Collections.nCopies(percent / division, "=")) +
                '>' +
                String.join("", Collections.nCopies(stringLength - percent / division, " ")) +
                ']' +
                String.join("", Collections.nCopies(item == 0 ? (int) (Math.log10(HALF_DAY_TICKS)) : (int) (Math.log10(HALF_DAY_TICKS)) - (int) (Math.log10(item)), " ")) +
                String.format(" %d/%d", item, HALF_DAY_TICKS);
    }

    private static long getWorldtime(long customtime, double multiplier)
    {
        return (long) ((customtime / multiplier) % 2147483647L);
    }

    private static double getMultiplier(boolean day)
    {
        return BigDecimal.valueOf((double) (day ? Config.day_length_seconds.get() : Config.night_length_seconds.get()) / vanilla_multiplier).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    //Because reasons
    public static boolean isDaytime(long worldtime)
    {
        final long l = worldtime % DAY_TICKS;

        return l >= 0 && l < HALF_DAY_TICKS;
    }

    public static long getSystemtimeTicks(int hour, int minute, int day)
    {
        hour = (hour - real_life_hour_offset + hours_per_day) % hours_per_day * ticks_per_hour;
        minute = (int) Math.round(minute * real_life_minute_multiplier % ticks_per_hour);

        return (hour + minute) + (day * DAY_TICKS);
    }
}
