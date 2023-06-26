package com.unixkitty.timecontrol;

import com.unixkitty.timecontrol.network.ModNetworkDispatcher;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TimeControl.MODID)
public class TimeControl
{
    // The MODID value here should match an entry in the META-INF/mods.toml file
    public static final String MODID = "timecontrol";

    public static final Logger LOG = LogManager.getLogger();

    public static GameRules.Key<GameRules.BooleanValue> DO_DAYLIGHT_CYCLE_TC = null;

    public TimeControl()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(TimeControl::onCommonSetup);
    }

    public static void onCommonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(ModNetworkDispatcher::register);

        if (DO_DAYLIGHT_CYCLE_TC == null)
        {
            DO_DAYLIGHT_CYCLE_TC = GameRules.register("doDaylightCycle_tc", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));

            LOG.info("Registered custom gamerule");
        }

        MinecraftForge.EVENT_BUS.addListener(TimeControl::onRegisterCommands);
    }

    public static void onRegisterCommands(final RegisterCommandsEvent event)
    {
        CommandTimeControl.register(event.getDispatcher());
    }
}
