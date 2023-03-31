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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Optional;

import static net.minecraft.world.level.GameRules.RULE_DAYLIGHT;

public class TimeEvents
{
    public static GameRules.Key<GameRules.BooleanValue> DO_DAYLIGHT_CYCLE_TC = null;

    private static final ITimeHandler SERVER = new ServerTimeHandler();
    private static final ITimeHandler CLIENT = new ClientTimeHandler();

    private static final String TIME_STRING = "time";
    private static final String ACTION_ADD = "add";
    private static final String ACTION_SET = "set";

    /*
        BEGIN modEventBus
     */

    public static void onCommonSetup(FMLCommonSetupEvent event)
    {
        MessageHandler.init();
        StartupEvents.initGamerule();
        MinecraftForge.EVENT_BUS.addListener(StartupEvents::onRegisterCommands);
    }

    /*
        BEGIN MinecraftForge.EVENT_BUS
     */

    //TODO implement custom time multipliers for dimensions other than the Overoworld using world.getDimensionType().doesFixedTimeExist()
    public static void onWorldLoad(LevelEvent.Load event)
    {
        if (DO_DAYLIGHT_CYCLE_TC == null || !(event.getLevel() instanceof Level world)) return;

        if (world.dimension() == Level.OVERWORLD)
        {
            MinecraftServer server = null;

            if (!world.isClientSide() && world instanceof ServerLevel)
            {
                server = ((ServerLevel) world).getServer();
            }

            world.getGameRules().getRule(RULE_DAYLIGHT).set(false, server);

            if (!world.isClientSide() && !Config.sync_to_system_time.get())
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
                        && event.player.level.dimension() == Level.OVERWORLD

        )
        {
            CLIENT.tick(event.player.level);
        }
    }

    public static void onWorldTick(TickEvent.LevelTickEvent event)
    {
        if (
                event.side == LogicalSide.SERVER
                        && event.phase == TickEvent.Phase.START
                        && event.level.dimension() == Level.OVERWORLD
        )
        {
            SERVER.tick(event.level);
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

                final CommandSourceStack sender = event.getParseResults().getContext().getSource();

                if (Config.sync_to_system_time.get())
                {
                    sender.sendFailure(new CommandRuntimeException(Component.translatable("text.timecontrol.change_time_when_system", action, TIME_STRING)).getComponent());
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
                    updateServer(action.equals(ACTION_SET) ? time.get() : sender.getLevel().getDayTime() + time.get());

                    //We cancel on success
                    event.setCanceled(true);
                }
            }
        }
    }

    public static void onSleepFinished(SleepFinishedTimeEvent event)
    {
        if (event.getLevel() instanceof ServerLevel world)
        {
            /*int dayTime = (int) (world.getDayTime() % 24000L);
            int newTime = (int) (event.getNewTime() % 24000L);*/

            //Adapted from mod Comforts for it's hammocks
            if (ModList.get().isLoaded("comforts"))
            {
                final boolean[] activeHammock = {true};

                for (Player player : event.getLevel().players())
                {
                    player.getSleepingPos().ifPresent(bedPos -> {
                        if (player.isSleepingLongEnough())
                        {
                            if (!Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(world.getBlockState(bedPos).getBlock())).toString().startsWith("comforts:hammock_"))
                            {
                                activeHammock[0] = false;
                            }
                        }
                    });

                    if (!activeHammock[0])
                    {
                        break;
                    }
                }

                if (activeHammock[0] && world.isDay())
                {
                    final long t = ((world.getDayTime() + 24000L) - (world.getDayTime() + 24000L) % 24000L) - 12001L;

                    event.setTimeAddition(t);
                    //nothing works without this line
                    updateServer(t);
                }
            }

//            TimeControl.log().debug("onSleepFinished called: dayTime " + dayTime + " | newTime " + newTime);

            //We reset the rule back after letting vanilla handle wakey-wakey
            if (((ServerLevel) event.getLevel()).getGameRules().getBoolean(DO_DAYLIGHT_CYCLE_TC))
            {
                ((ServerLevel) event.getLevel()).getGameRules().getRule(RULE_DAYLIGHT).set(false, ((ServerLevel) event.getLevel()).getServer());
            }
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
            ClientLevel world = Minecraft.getInstance().level;

            if (world != null)
            {
                world.getGameRules().getRule(RULE_DAYLIGHT).set(((GameruleMessageToClient) updateMessage).getVanillaRule(), null);
                world.getGameRules().getRule(DO_DAYLIGHT_CYCLE_TC).set(((GameruleMessageToClient) updateMessage).getModRule(), null);
            }
        }
    }

    public static void updateServer(long worldtime)
    {
        SERVER.update(Numbers.customtime(worldtime), Numbers.multiplier(worldtime));
    }
}