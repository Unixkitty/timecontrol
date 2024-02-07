package com.unixkitty.timecontrol.config;

import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.config.json.JsonConfig;

public class Config
{
    private static JsonConfig CONFIG = null;

    private static boolean server = false;

    /* BEGIN ENTRIES */
    public static JsonConfig.BooleanValue debug;

    public static JsonConfig.BooleanValue ignore_server;
    public static JsonConfig.BooleanValue sync_to_system_time;
    public static JsonConfig.IntValue sync_to_system_time_rate;

    public static JsonConfig.IntValue day_length_seconds;
    public static JsonConfig.IntValue night_length_seconds;
    /* END ENTRIES */

    public static void load()
    {
        if (CONFIG == null)
        {
            CONFIG = new JsonConfig(TimeControl.MODID);

            ignore_server = CONFIG.defineValue("ignore_server", false);
            sync_to_system_time = CONFIG.defineValue("sync_to_system_time", false);
            sync_to_system_time_rate = CONFIG.defineInRange("sync_to_system_time_rate", 20, 1, 864000);

            day_length_seconds = CONFIG.defineInRange("day_length_seconds", 600, 4, 10737360);
            night_length_seconds = CONFIG.defineInRange("night_length_seconds", 600, 4, 10737360);

            debug = CONFIG.defineValue("debug", false);
        }

        CONFIG.load();
    }

    public static void save()
    {
        if (CONFIG != null)
        {
            CONFIG.save();
        }
    }

    public static void close()
    {
        if (server && CONFIG != null)
        {
            CONFIG.close();
        }
    }

    public static synchronized void setClient()
    {
        server = false;
    }
}
