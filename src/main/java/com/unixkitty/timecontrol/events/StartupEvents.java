package com.unixkitty.timecontrol.events;

import com.unixkitty.timecontrol.CommandTimeControl;
import com.unixkitty.timecontrol.TimeControl;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.RegisterCommandsEvent;

@SuppressWarnings("unused")
public class StartupEvents
{
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        CommandTimeControl.register(event.getDispatcher());
    }

    static void initGamerule()
    {
        if (TimeEvents.DO_DAYLIGHT_CYCLE_TC != null)
        {
            return;
        }

        TimeEvents.DO_DAYLIGHT_CYCLE_TC = GameRules.register("doDaylightCycle_tc", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));

        TimeControl.log().info("Registered custom gamerule");
    }
}
