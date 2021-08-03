package com.unixkitty.timecontrol;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@SuppressWarnings("CanBeFinal")
@Mod.EventBusSubscriber(modid = TimeControl.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    public static ForgeConfigSpec COMMON_CONFIG;
    //public static ForgeConfigSpec CLIENT_CONFIG; This will be needed for client-specific options

    private static final int SYNC_TO_SYSTEM_TIME_RATE_LIMIT = 864000;
    private static final int LENGTH_LIMIT = 178956;

    /* BEGIN ENTRIES */

    public static final String CATEGORY_MISC = "miscellaneous";

    public static ForgeConfigSpec.BooleanValue debugMode;

    public static final String CATEGORY_SYSTEM = "system_time";

    public static ForgeConfigSpec.BooleanValue sync_to_system_time;
    public static ForgeConfigSpec.IntValue sync_to_system_time_rate;

    public static final String CATEGORY_ARBITRARY = "arbitrary_time";

    public static ForgeConfigSpec.IntValue day_length_minutes;
    public static ForgeConfigSpec.IntValue night_length_minutes;

    /* END ENTRIES */

    static
    {
        ForgeConfigSpec.Builder commonConfig = new ForgeConfigSpec.Builder();

        {
            commonConfig.push(CATEGORY_SYSTEM);
            sync_to_system_time = commonConfig.comment("Synchronize game world time with system time").define("sync_to_system_time", false);
            sync_to_system_time_rate = commonConfig.comment("Sync time every n ticks").defineInRange("sync_to_system_time_rate", 20, 1, SYNC_TO_SYSTEM_TIME_RATE_LIMIT);
            commonConfig.pop();
        }

        {
            commonConfig.push(CATEGORY_ARBITRARY);
            day_length_minutes = commonConfig.comment("How long daytime lasts (0 - " + Numbers.night_start + ")").defineInRange("day_length_minutes", 10, 1, LENGTH_LIMIT);
            night_length_minutes = commonConfig.comment("How long nighttime lasts (" + Numbers.night_start + " - 24000)").defineInRange("night_length_minutes", 10, 1, LENGTH_LIMIT);
            commonConfig.pop();
        }

        {
            commonConfig.push(CATEGORY_MISC);
            debugMode = commonConfig.define("debugMode", false);
            commonConfig.pop();
        }

        COMMON_CONFIG = commonConfig.build();
    }

    private static void reload(ModConfig config)
    {
        COMMON_CONFIG.setConfig(config.getConfigData());
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading event)
    {
        reload(event.getConfig());
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onFileChange(final ModConfig.ConfigReloading event)
    {
        reload(event.getConfig());
    }
}
