package com.unixkitty.timecontrol.events;

import com.unixkitty.timecontrol.TimeControl;
import net.minecraft.command.impl.GameRuleCommand;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.GameRules;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class StartupEvents
{
    public static void onServerSetup(FMLServerAboutToStartEvent event)
    {
        if (event.getServer() instanceof DedicatedServer)
        {
            initGamerule(true);
        }

        GameRuleCommand.register(event.getServer().getCommandManager().getDispatcher());
    }

    static void initGamerule(boolean dedicatedServer)
    {
        if (TimeEvents.DO_DAYLIGHT_CYCLE_TC != null)
        {
            return;
        }

        Method createBoolean = ObfuscationReflectionHelper.findMethod(GameRules.BooleanValue.class, "func_223568_b", boolean.class);

        createBoolean.setAccessible(true);

        if (dedicatedServer)
        {
            _initGamerule(createBoolean);
        }
        else
        {
            DeferredWorkQueue.runLater(() -> _initGamerule(createBoolean));
        }
    }

    @SuppressWarnings("unchecked")
    private static void _initGamerule(Method method)
    {
        try
        {
            Object boolTrue = method.invoke(GameRules.BooleanValue.class, true);

            TimeEvents.DO_DAYLIGHT_CYCLE_TC = /* GameRules.register() */ GameRules.func_234903_a_("doDaylightCycle_tc", GameRules.Category.UPDATES, (GameRules.RuleType<GameRules.BooleanValue>) boolTrue);

            TimeControl.log().info("Registered custom gamerule");
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            TimeControl.log().error("Failed to create gamerule!");

            e.printStackTrace();
        }
    }
}
