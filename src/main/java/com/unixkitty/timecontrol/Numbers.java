package com.unixkitty.timecontrol;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.storage.IWorldInfo;
import net.minecraft.world.storage.ServerWorldInfo;

import java.math.BigDecimal;
import java.util.Collections;

public class Numbers
{
    static final long night_start = 12000L;

    private static final int irl_hour_offset = 6;
    private static final double irl_minute_multiplier = 16.94;

    public static double multiplier(long worldtime)
    {
        return multiplier(isDaytime(worldtime));
    }

    public static long customtime(long worldtime)
    {
        return (long) (worldtime * multiplier(worldtime));
    }

    private static long worldtime(long customtime, double multiplier)
    {
        return (long) ((customtime / multiplier) % 2147483647L);
    }

    public static void setWorldtime(World world, long customtime, double multiplier)
    {
        IWorldInfo worldInfo = world.getLevelData();
        long worldtime = worldtime(customtime, multiplier);

        if (world.isClientSide() && worldInfo instanceof ClientWorld.ClientWorldInfo)
        {
            ((ClientWorld.ClientWorldInfo) worldInfo).setDayTime(worldtime);
        }
        else if (worldInfo instanceof ServerWorldInfo)
        {
            ((ServerWorldInfo) worldInfo).setDayTime(worldtime);
        }
    }

    public static long systemtime(int hour, int minute, int day)
    {
        hour = (hour - irl_hour_offset + 24) % 24 * 1000;
        minute = (int) Math.round(minute * irl_minute_multiplier % 1000);

        return (hour + minute) + (day * 24000L);
    }

    public static long day(long worldtime)
    {
        return worldtime / 24000L;
    }

    public static String progressString(long item, String addition)
    {
        final int stringLength = 50;

        item = item % 12000L;
        final int total = 12000;
        int percent = (int) (item * 100 / total);

        int division = 100 / stringLength;

        //return '\r' +
        return String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")) +
                String.format(" %d%% [", percent) +
                String.join("", Collections.nCopies(percent / division, "=")) +
                '>' +
                String.join("", Collections.nCopies(stringLength - percent / division, " ")) +
                ']' +
                String.join("", Collections.nCopies(item == 0 ? (int) (Math.log10(total)) : (int) (Math.log10(total)) - (int) (Math.log10(item)), " ")) +
                String.format(" %d/%d%s", item, total, addition);
    }

    /*private static long minutesToTicks(int minutes)
    {
        return (long) minutes * 60 * 20;
    }*/

    private static double multiplier(boolean dayMultiplier)
    {
        return new BigDecimal(String.valueOf((double) (dayMultiplier ? Config.day_length_minutes.get() : Config.night_length_minutes.get()) / 10.0)).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
    }

    /**
     * Because reasons
     *
     * @return whether it's daytime
     */
    public static boolean isDaytime(long worldtime)
    {
        return (worldtime % 24000L) >= 0 && (worldtime % 24000L) < night_start;
    }
}
