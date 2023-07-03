package com.unixkitty.timecontrol;

import com.unixkitty.timecontrol.config.Config;
import com.unixkitty.timecontrol.handler.ServerTimeHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.level.GameRules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeControl implements ModInitializer
{
    public static final String MODID = "timecontrol";

    public static final Logger LOG = LogManager.getLogger(TimeControl.class);

    public static GameRules.Key<GameRules.BooleanValue> DO_DAYLIGHT_CYCLE_TC = null;

    @Override
    public void onInitialize()
    {
        Config.load();

        if (DO_DAYLIGHT_CYCLE_TC == null)
        {
            DO_DAYLIGHT_CYCLE_TC = GameRules.register("doDaylightCycle_tc", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));

            LOG.info("Registered custom gamerule");
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> TimeControlCommand.register(dispatcher));

        ServerWorldEvents.LOAD.register(new ServerTimeHandler.WorldLoad());
        ServerTickEvents.START_WORLD_TICK.register(new ServerTimeHandler.WorldTick());

//        Config.setClient();
    }
}