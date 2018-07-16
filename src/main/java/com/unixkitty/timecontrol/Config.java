package com.unixkitty.timecontrol;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class Config
{
    private static final String CATEGORY_SYSTEM = "System time";
    private static final String CATEGORY_ARBITRARY = "Arbitrary time";

    private static final int sync_to_system_time_rate_limit = 864000;
    private static final int length_limit = 178956;

    private static boolean debug = false;

    private static boolean sync_to_system_time = true;
    private static int sync_to_system_time_rate = 20;

    private static int day_length_minutes = 10;
    private static int night_length_minutes = 10;

    static void load(File file)
    {
        Configuration config = new Configuration(file, TimeControl.VERSION);

        debug = config.getBoolean("debug", "DEBUG", debug, "Print debug info to console");

        sync_to_system_time = config.getBoolean("sync_to_system_time", CATEGORY_SYSTEM, sync_to_system_time, "Synchronize in-world time with system time");
        sync_to_system_time_rate = config.getInt("sync_to_system_time_rate", CATEGORY_SYSTEM, sync_to_system_time_rate, 1, sync_to_system_time_rate_limit, "Sync time every n ticks");

        day_length_minutes = config.getInt("day_length_minutes", CATEGORY_ARBITRARY, day_length_minutes, 1, length_limit, "How long daytime lasts (0 - " + Numbers.night_start + ")");
        night_length_minutes = config.getInt("night_length_minutes", CATEGORY_ARBITRARY, night_length_minutes, 1, length_limit, "How long nighttime lasts (" + Numbers.night_start + " - 24000)");

        config.save();
    }

    static int dayLength()
    {
        return day_length_minutes;
    }

    static int nightLength()
    {
        return night_length_minutes;
    }

    public static boolean debugMode()
    {
        return debug;
    }

    public static boolean sync_to_system()
    {
        return sync_to_system_time;
    }

    public static int sync_to_system_rate()
    {
        return sync_to_system_time_rate;
    }
}
