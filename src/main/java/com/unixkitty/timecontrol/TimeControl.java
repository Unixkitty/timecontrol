package com.unixkitty.timecontrol;

import com.unixkitty.timecontrol.events.StartupEvents;
import com.unixkitty.timecontrol.events.TimeEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

//        MinecraftForge.EVENT_BUS.register(TimeEvents.class);

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.addListener(StartupEvents::onServerSetup);
        MinecraftForge.EVENT_BUS.addListener(TimeEvents::onWorldLoad);

        modEventBus.addListener(TimeEvents::onClientSetup);
        modEventBus.addListener(TimeEvents::onCommonSetup);

        MinecraftForge.EVENT_BUS.addListener(TimeEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(TimeEvents::onWorldTick);
        MinecraftForge.EVENT_BUS.addListener(TimeEvents::onCommand);
        MinecraftForge.EVENT_BUS.addListener(TimeEvents::onSleepFinished);
    }

    public static Logger log()
    {
        return LOG;
    }
}
