package com.unixkitty.timecontrol;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@SuppressWarnings("CanBeFinal")
@Mod.EventBusSubscriber(modid = TimeControl.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    static ForgeConfigSpec SERVER_CONFIG;
    static ForgeConfigSpec CLIENT_CONFIG;

    public static final int SYNC_TO_SYSTEM_TIME_RATE_LIMIT = 864000;
    public static final int LENGTH_LIMIT = 178956;

    /* BEGIN ENTRIES */
    public static final String CATEGORY_MISC = "miscellaneous";

    public static final String DEBUG = "debug";
    public static ForgeConfigSpec.BooleanValue debug;
    public static ForgeConfigSpec.BooleanValue clientDebug;

    public static final String CATEGORY_SYSTEM = "system_time";

    public static final String SYNC_TO_SYSTEM_TIME = "sync_to_system_time";
    public static ForgeConfigSpec.BooleanValue sync_to_system_time;

    public static final String SYNC_TO_SYSTEM_TIME_RATE = "sync_to_system_time_rate";
    public static ForgeConfigSpec.IntValue sync_to_system_time_rate;

    public static final String CATEGORY_ARBITRARY = "arbitrary_time";

    public static final String DAY_LENGTH_MINUTES = "day_length_minutes";
    public static ForgeConfigSpec.IntValue day_length_minutes;

    public static final String NIGHT_LENGTH_MINUTES = "night_length_minutes";
    public static ForgeConfigSpec.IntValue night_length_minutes;
    /* END ENTRIES */

    static
    {
        ForgeConfigSpec.Builder config = new ForgeConfigSpec.Builder();

        {
            config.push(CATEGORY_SYSTEM);
            sync_to_system_time = config.comment("Synchronize game world time with system time").define(SYNC_TO_SYSTEM_TIME, false);
            sync_to_system_time_rate = config.comment("Sync time every n ticks").defineInRange(SYNC_TO_SYSTEM_TIME_RATE, 20, 1, SYNC_TO_SYSTEM_TIME_RATE_LIMIT);
            config.pop();
        }

        {
            config.push(CATEGORY_ARBITRARY);
            day_length_minutes = config.comment("How long daytime lasts (0 - " + Numbers.HALF_DAY_TICKS + ")").defineInRange(DAY_LENGTH_MINUTES, 10, 1, LENGTH_LIMIT);
            night_length_minutes = config.comment("How long nighttime lasts (" + Numbers.HALF_DAY_TICKS + " - 24000)").defineInRange(NIGHT_LENGTH_MINUTES, 10, 1, LENGTH_LIMIT);
            config.pop();
        }

        {
            config.push(CATEGORY_MISC);
            debug = config.define(DEBUG, false);
            config.pop();
        }

        SERVER_CONFIG = config.build();

        ForgeConfigSpec.Builder clientConfig = new ForgeConfigSpec.Builder();

        clientDebug = clientConfig.define(DEBUG, false);

        CLIENT_CONFIG = clientConfig.build();
    }

    private static void reload(ModConfig config, ModConfig.Type type)
    {
        if (config.getModId().equals(TimeControl.MODID))
        {
            if (type == ModConfig.Type.SERVER)
            {
                SERVER_CONFIG.setConfig(config.getConfigData());
            }
            else if (type == ModConfig.Type.CLIENT)
            {
                CLIENT_CONFIG.setConfig(config.getConfigData());
            }
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event)
    {
        reload(event.getConfig(), event.getConfig().getType());
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onFileChange(final ModConfigEvent.Reloading event)
    {
        reload(event.getConfig(), event.getConfig().getType());
    }
}
