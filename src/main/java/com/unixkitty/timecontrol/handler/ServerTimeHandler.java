package com.unixkitty.timecontrol.handler;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.config.Config;
import com.unixkitty.timecontrol.event.CommandEvent;
import com.unixkitty.timecontrol.event.SleepFinishedTimeEvent;
import com.unixkitty.timecontrol.network.ModNetworkDispatcher;
import com.unixkitty.timecontrol.network.packet.GamerulesS2CPacket;
import com.unixkitty.timecontrol.network.packet.TimeS2CPacket;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.minecraft.world.level.GameRules.RULE_DAYLIGHT;

public final class ServerTimeHandler extends TimeHandler
{
    private static final ServerTimeHandler instance = new ServerTimeHandler();
    private static final Logger log = LogManager.getLogger(ServerTimeHandler.class);

    private static final String time_string = "time";
    private static final String add_string = "add";
    private static final String set_string = "set";

    /* Arbitrary time */
    private long lastCustomtime = this.customtime;

    private ServerTimeHandler()
    {
    }

    @Override
    public void tick(@NotNull Level level)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            if (Config.sync_to_system_time.get())
            {
                if (!serverLevel.isClientSide && serverLevel.getServer().getTickCount() % Config.sync_to_system_time_rate.get() == 0)
                {
                    syncTimeWithSystem(serverLevel);
                }
            }
            else
            {
                long worldtime = serverLevel.getDayTime();

                boolean isDaytime = Numbers.isDaytime(worldtime);

                if (isDaytime != this.wasDaytime)
                {
                    reset(serverLevel, worldtime);

                    this.wasDaytime = isDaytime;
                }

                if (serverLevel.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC))
                {
                    if (areAllPlayersAsleep(serverLevel))
                    {
                        serverLevel.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, serverLevel.getServer());
                    }

                    this.lastCustomtime = ++this.customtime;

                    Numbers.setWorldtime(serverLevel, this.customtime, this.multiplier);
                }

                if (serverLevel.getServer().getTickCount() % 20 == 0)
                {
                    //Detect config changes
                    if (this.dayMinutes != Config.day_length_minutes.get() || this.nightMinutes != Config.night_length_minutes.get())
                    {
                        TimeHandler.update(instance, serverLevel, serverLevel.getDayTime());
                    }

                    //This is to keep client multipliers in sync
                    updateClientsTime(serverLevel);
                    ModNetworkDispatcher.send(serverLevel, new GamerulesS2CPacket(serverLevel));

                    if (Config.debug.get())
                    {
                        long updatedWorldtime = serverLevel.getDayTime();

                        log.debug(
                                "{} Server time update: {} -> {} ({} -> {}) (day {}) | multiplier: {}",
                                Numbers.getProgressString(updatedWorldtime),
                                worldtime,
                                updatedWorldtime,
                                this.lastCustomtime,
                                this.customtime,
                                updatedWorldtime / Numbers.DAY_TICKS,
                                this.multiplier
                        );
                    }
                }
            }
        }
    }

    @Override
    public void update(@Nullable Level level, long customtime, double multiplier)
    {
        super.update(level, customtime, multiplier);

        if (level instanceof ServerLevel serverLevel)
        {
            updateClientsTime(serverLevel);
        }
    }

    private double getMultiplier()
    {
        return this.multiplier;
    }

    private long getCustomtime()
    {
        return this.customtime;
    }

    private boolean areAllPlayersAsleep(ServerLevel level)
    {
        final int l = level.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);

        return level.sleepStatus.areEnoughSleeping(l) && level.sleepStatus.areEnoughDeepSleeping(l, level.players());
    }

    private void reset(ServerLevel level, long worldtime)
    {
        update(level, Numbers.getCustomTime(worldtime), Numbers.getMultiplier(worldtime));
    }

    private void updateClientsTime(ServerLevel level)
    {
        ModNetworkDispatcher.send(level, new TimeS2CPacket(this.customtime, this.multiplier));
    }

    public static class WorldLoad implements ServerWorldEvents.Load
    {
        @Override
        public void onWorldLoad(MinecraftServer server, ServerLevel level)
        {
            if (TimeControl.DO_DAYLIGHT_CYCLE_TC != null && level.dimension() == Level.OVERWORLD)
            {
                level.getGameRules().getRule(RULE_DAYLIGHT).set(false, level.getServer());

                if (!Config.sync_to_system_time.get())
                {
                    update(instance, level, level.getDayTime());
                }
            }
        }
    }

    public static class WorldTick implements ServerTickEvents.StartWorldTick
    {
        @Override
        public void onStartTick(ServerLevel level)
        {
            if (level.dimension() == Level.OVERWORLD)
            {
                instance.tick(level);
            }
        }
    }

    public static void onCommand(final CommandEvent event)
    {
        final CommandSourceStack sender = event.getParseResults().getContext().getSource();
        final ServerLevel level = sender.getLevel();

        if (TimeControl.DO_DAYLIGHT_CYCLE_TC != null && event.getException() == null && event.getParseResults().getReader().getString().contains(time_string) && level.dimension() == Level.OVERWORLD)
        {
            final CommandContext<?> context = event.getParseResults().getContext().build(event.getParseResults().getReader().getString());

            if (context.hasNodes() && context.getNodes().get(0).getNode().getName().equals(time_string) && context.getNodes().size() == 3)
            {
                final String action = context.getNodes().get(1).getNode().getName();

                if (!action.equals(set_string) && !action.equals(add_string)) return;

                if (Config.sync_to_system_time.get())
                {
                    event.setException(new SimpleCommandExceptionType(Component.translatable("commands.timecontrol.change_time_when_system", action, time_string)).create());
                    event.setCanceled(true);

                    return;
                }

                final String argument = context.getNodes().get(2).getNode().getName();

                Integer time = null;

                switch (argument)
                {
                    case "day" -> time = 1000;
                    case "noon" -> time = 6000;
                    case "night" -> time = 13000;
                    case "midnight" -> time = 18000;
                    case time_string ->
                    {
                        try
                        {
                            time = IntegerArgumentType.getInteger(context, time_string);
                        }
                        catch (IllegalArgumentException ignored)
                        {

                        }
                    }
                }

                if (Config.debug.get())
                {
                    TimeControl.LOG.debug("Caught time command: /" + time_string + " " + action + " " + (time == null ? argument : time));
                }

                if (time != null)
                {
                    update(instance, level, action.equals(set_string) ? time : level.getDayTime() + time);

                    Numbers.setWorldtime(level, instance.getCustomtime(), instance.getMultiplier());

                    //We cancel on success
                    event.setCanceled(true);
                }
            }
        }
    }

    public static void onSleepFinished(final SleepFinishedTimeEvent event)
    {
        ServerLevel level = event.getLevel();

        //Adapted from mod Comforts for it's hammocks
        if (FabricLoader.getInstance().isModLoaded("comforts"))
        {
            final boolean[] activeHammock = {true};

            for (Player player : event.getLevel().players())
            {
                player.getSleepingPos().ifPresent(bedPos -> {
                    if (player.isSleepingLongEnough())
                    {
                        if (!Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(level.getBlockState(bedPos).getBlock())).toString().startsWith("comforts:hammock_"))
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

            if (activeHammock[0] && level.isDay())
            {
                final long t = ((level.getDayTime() + 24000L) - (level.getDayTime() + 24000L) % 24000L) - 12001L;

                if (event.setTimeAddition(t))
                {
                    //nothing works without this line
                    update(instance, level, t);
                }
                else
                {
                    TimeControl.LOG.warn("Somehow tried to daysleep on a hammock backwards in time?");
                }
            }
        }

        //We reset the rule back after letting vanilla handle wakey-wakey
        if (level.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC))
        {
            level.getGameRules().getRule(RULE_DAYLIGHT).set(false, level.getServer());
        }
    }
}
