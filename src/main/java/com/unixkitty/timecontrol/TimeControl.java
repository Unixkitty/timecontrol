package com.unixkitty.timecontrol;

import com.unixkitty.timecontrol.network.MessageHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@SuppressWarnings("WeakerAccess")
@Mod(modid = TimeControl.MODID, name = TimeControl.NAME, version = TimeControl.VERSION, acceptedMinecraftVersions = "[1.12]")
public class TimeControl
{
    public static final String MODID = "timecontrol";
    public static final String NAME = "TimeControl";
    //MCVERSION-MAJORMOD.MAJORAPI.MINOR.PATCH
    public static final String VERSION = "1.12.2-1.0.0.0-beta";

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        MessageHandler.init();
        Config.load(event.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(TimeEvents.INSTANCE);
    }
}
