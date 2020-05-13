package com.unixkitty.timecontrol.events;

import com.unixkitty.timecontrol.TimeControl;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber
public class StartupEvents
{

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        initGamerule(false);
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    @SubscribeEvent
    public static void onServerSetup(FMLServerAboutToStartEvent event)
    {
        initGamerule(true);

        try
        {
            Field commandManagerField = ObfuscationReflectionHelper.findField(MinecraftServer.class, "field_195579_af");

            commandManagerField.setAccessible(true);

            commandManagerField.set(event.getServer(), new Commands(true));
        }
        catch (IllegalAccessException e)
        {
            TimeControl.log().error("Failed to reinstantiate Commands on dedicated server, may not be possible to set custom gamerule in-game");

            e.printStackTrace();
        }
    }

    private static void initGamerule(boolean dedicatedServer)
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

    private static void _initGamerule(Method method)
    {
        try
        {
            Object boolTrue = method.invoke(GameRules.BooleanValue.class, true);

            TimeEvents.DO_DAYLIGHT_CYCLE_TC = GameRules.register("doDaylightCycle_tc", (GameRules.RuleType<GameRules.BooleanValue>) boolTrue);

            TimeControl.log().info("Registered custom gamerule");
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            TimeControl.log().error("Failed to create gamerule!");

            e.printStackTrace();
        }
    }
}
