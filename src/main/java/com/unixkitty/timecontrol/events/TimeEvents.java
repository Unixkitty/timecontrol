package com.unixkitty.timecontrol.events;

import com.mojang.brigadier.context.CommandContext;
import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.handler.ClientTimeHandler;
import com.unixkitty.timecontrol.handler.ITimeHandler;
import com.unixkitty.timecontrol.handler.ServerTimeHandler;
import com.unixkitty.timecontrol.network.MessageHandler;
import com.unixkitty.timecontrol.network.message.GameruleMessageToClient;
import com.unixkitty.timecontrol.network.message.IMessage;
import com.unixkitty.timecontrol.network.message.TimeMessageToClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.TimeCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.SleepFinishedTimeEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Optional;

import static net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE;

@SuppressWarnings("unused")
public class TimeEvents
{
    public static GameRules.RuleKey<GameRules.BooleanValue> DO_DAYLIGHT_CYCLE_TC = null;

    private static final ITimeHandler SERVER = new ServerTimeHandler();
    private static final ITimeHandler CLIENT = new ClientTimeHandler();

    private static final String TIME_STRING = "time";
    private static final String ACTION_ADD = "add";
    private static final String ACTION_SET = "set";

    /*
        BEGIN modEventBus
     */

    public static void onClientSetup(FMLClientSetupEvent event)
    {
        StartupEvents.initGamerule(false);
    }

    public static void onCommonSetup(FMLCommonSetupEvent event)
    {
        StartupEvents.initGamerule(false);
        MessageHandler.init();
        MinecraftForge.EVENT_BUS.addListener(StartupEvents::onRegisterCommands);
    }

    /*
        BEGIN MinecraftForge.EVENT_BUS
     */

    //TODO implement custom time multipliers for dimensions other than the Overoworld using world.getDimensionType().doesFixedTimeExist()
    public static void onWorldLoad(WorldEvent.Load event)
    {
        if (DO_DAYLIGHT_CYCLE_TC == null || !(event.getWorld() instanceof World)) return;

        World world = (World) event.getWorld();

        if (world.getDimensionKey() == World.OVERWORLD)
        {
            MinecraftServer server = null;

            if (!world.isRemote() && world instanceof ServerWorld)
            {
                server = ((ServerWorld) world).getServer();
            }

            world.getGameRules().get(DO_DAYLIGHT_CYCLE).set(false, server);

            if (!world.isRemote() && !Config.sync_to_system_time.get())
            {
                updateServer(world.getDayTime());
            }
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (
                event.side == LogicalSide.CLIENT
                        && event.phase == TickEvent.Phase.START
//                        && !event.player.world.getDimensionType().doesFixedTimeExist()
                        && event.player.world.getDimensionKey() == World.OVERWORLD

        )
        {
            CLIENT.tick(event.player.world);
        }
    }

    public static void onWorldTick(TickEvent.WorldTickEvent event)
    {
        if (
                event.side == LogicalSide.SERVER
                        && event.phase == TickEvent.Phase.START
                        && event.world.getDimensionKey() == World.OVERWORLD
        )
        {
            SERVER.tick(event.world);
        }
    }

    public static void onCommand(CommandEvent event)
    {
        if (DO_DAYLIGHT_CYCLE_TC != null && event.getException() == null && event.getParseResults().getReader().getString().contains(TIME_STRING))
        {
            final CommandContext<?> context = event.getParseResults().getContext().build(event.getParseResults().getReader().getString());

            if (context.hasNodes() && context.getNodes().get(0).getNode().getName().equals(TIME_STRING) && context.getNodes().size() == 3)
            {
                final String action = context.getNodes().get(1).getNode().getName();

                if (!action.equals(ACTION_SET) && !action.equals(ACTION_ADD)) return;

                final CommandSource sender = event.getParseResults().getContext().getSource();

                if (Config.sync_to_system_time.get())
                {
                    sender.sendErrorMessage(new CommandException(new TranslationTextComponent("text.timecontrol.change_time_when_system", action, TIME_STRING)).getComponent());
                    event.setCanceled(true);
                    return;
                }

                final String argument = context.getNodes().get(2).getNode().getName();

                TimeControl.log().debug("Caught time command: /" + TIME_STRING + " " + action + " " + argument);

                Optional<Integer> time = Optional.empty();

                switch (argument)
                {
                    case "day":
                        time = Optional.of(1000);
                        break;
                    case "noon":
                        time = Optional.of(6000);
                        break;
                    case "night":
                        time = Optional.of(13000);
                        break;
                    case "midnight":
                        time = Optional.of(18000);
                        break;
                    case TIME_STRING:
                        try
                        {
                            time = Optional.of(context.getArgument(TIME_STRING, Integer.class));
                        }
                        catch (IllegalArgumentException ignored)
                        {

                        }
                        break;
                }

                if (time.isPresent())
                {
                    updateServer(
                            action.equals(ACTION_SET) ?
                                    TimeCommand.setTime(sender, time.get()) :
                                    TimeCommand.addTime(sender, time.get())
                    );

                    //We cancel on success
                    event.setCanceled(true);
                }
            }
        }
    }

    public static void onSleepFinished(SleepFinishedTimeEvent event)
    {
        if (event.getWorld() instanceof ServerWorld)
        {
            updateServer(event.getNewTime());
        }
    }

    public static void updateClient(IMessage updateMessage)
    {
        if (updateMessage instanceof TimeMessageToClient)
        {
            CLIENT.update(((TimeMessageToClient) updateMessage).getCustomtime(), ((TimeMessageToClient) updateMessage).getMultiplier());
        }

        if (updateMessage instanceof GameruleMessageToClient)
        {
            ClientWorld world = Minecraft.getInstance().world;

            if (world != null)
            {
                world.getGameRules().get(DO_DAYLIGHT_CYCLE).set(((GameruleMessageToClient) updateMessage).getVanillaRule(), null);
                world.getGameRules().get(DO_DAYLIGHT_CYCLE_TC).set(((GameruleMessageToClient) updateMessage).getModRule(), null);
            }
        }
    }

    public static void updateServer(long worldtime)
    {
        SERVER.update(Numbers.customtime(worldtime), Numbers.multiplier(worldtime));
    }
}