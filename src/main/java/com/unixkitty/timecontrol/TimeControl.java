package com.unixkitty.timecontrol;

import com.unixkitty.timecontrol.events.TimeEvents;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TimeControl.MODID)
public class TimeControl
{
    // The MODID value here should match an entry in the META-INF/mods.toml file
    public static final String MODID = "timecontrol";
    public static final String MODNAME = "TimeControl";

    private static final Logger LOG = LogManager.getLogger(MODNAME);

    public TimeControl()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);

        MinecraftForge.EVENT_BUS.register(TimeEvents.class);
        MinecraftForge.EVENT_BUS.register(ForgeConfig.class);
    }

    public static Logger log()
    {
        return LOG;
    }
}
