package com.unixkitty.timecontrol.handler;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.network.ModNetworkDispatcher;
import com.unixkitty.timecontrol.network.packet.GamerulesS2CPacket;
import com.unixkitty.timecontrol.network.packet.TimeS2CPacket;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import static net.minecraft.world.level.GameRules.RULE_DAYLIGHT;

@Mod.EventBusSubscriber(modid = TimeControl.MODID)
public final class ServerTimeHandler extends TimeHandler
{
    private static final ServerTimeHandler instance = new ServerTimeHandler();
    private static final Logger log = LogManager.getLogger(ServerTimeHandler.class.getSimpleName());

    private static final String time_string = "time";
    private static final String add_string = "add";
    private static final String set_string = "set";

    /* System time */
    private int lastMinute = 0;

    /* Arbitrary time */
    private boolean wasDaytime = true;
    private int dayMinutes = 0;
    private int nightMinutes = 0;
    private long lastCustomtime = this.customtime;

    private ServerTimeHandler()
    {
    }

    @Override
    public void tick(@Nonnull Level level)
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
                    reset(worldtime);

                    this.wasDaytime = isDaytime;
                }

                if (serverLevel.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC))
                {
                    //TODO what about Quark's "Improved Sleeping"?
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
                    if (this.dayMinutes != Config.day_length_seconds.get() || this.nightMinutes != Config.night_length_seconds.get())
                    {
                        ServerTimeHandler.update(serverLevel.getDayTime());
                    }

                    //This is to keep client multipliers in sync
                    updateClientsTime();
                    ModNetworkDispatcher.send(new GamerulesS2CPacket(serverLevel), serverLevel.dimension());

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
    public void update(long customtime, double multiplier)
    {
        super.update(customtime, multiplier);

        updateClientsTime();
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

    private void reset(long worldtime)
    {
        update(Numbers.getCustomTime(worldtime), Numbers.getMultiplier(worldtime));
    }

    private void syncTimeWithSystem(ServerLevel level)
    {
        LocalTime now = LocalTime.now();
        int minute = now.getMinute();

        if (minute != this.lastMinute)
        {
            long worldTime = level.getDayTime();
            int hour = now.getHour();
            int day = LocalDate.now().getDayOfYear();
            long time = Numbers.getSystemtimeTicks(hour, minute, day);

            this.lastMinute = minute;

            level.setDayTime(time);

            if (Config.debug.get())
            {
                log.debug("System time update: {} -> {} | day {}, {}", worldTime, time, day, String.format("%02d:%02d", hour, minute));
            }
        }
    }

    private void updateClientsTime()
    {
        ModNetworkDispatcher.send(new TimeS2CPacket(this.customtime, this.multiplier), Level.OVERWORLD);
    }

    //TODO implement custom time multipliers for dimensions other than the Overoworld using world.getDimensionType().doesFixedTimeExist()
    @SubscribeEvent
    public static void onWorldLoad(final LevelEvent.Load event)
    {
        if (TimeControl.DO_DAYLIGHT_CYCLE_TC != null && event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD)
        {
            level.getGameRules().getRule(RULE_DAYLIGHT).set(false, level.getServer());

            if (!Config.sync_to_system_time.get())
            {
                update(level.getDayTime());
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(final TickEvent.LevelTickEvent event)
    {
        if (
                event.side == LogicalSide.SERVER
                        && event.phase == TickEvent.Phase.START
                        && event.level.dimension() == Level.OVERWORLD
        )
        {
            instance.tick(event.level);
        }
    }

    @SubscribeEvent
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
                    sender.sendFailure(new CommandRuntimeException(Component.translatable("commands.timecontrol.change_time_when_system", action, time_string)).getComponent());
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
                    update(action.equals(set_string) ? time : level.getDayTime() + time);

                    Numbers.setWorldtime(level, instance.getCustomtime(), instance.getMultiplier());

                    //We cancel on success
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onSleepFinished(final SleepFinishedTimeEvent event)
    {
        if (event.getLevel() instanceof ServerLevel level)
        {
            //Adapted from mod Comforts for it's hammocks
            if (ModList.get().isLoaded("comforts"))
            {
                final boolean[] activeHammock = {true};

                for (Player player : event.getLevel().players())
                {
                    player.getSleepingPos().ifPresent(bedPos -> {
                        if (player.isSleepingLongEnough())
                        {
                            if (!Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(level.getBlockState(bedPos).getBlock())).toString().startsWith("comforts:hammock_"))
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

                    event.setTimeAddition(t);

                    //nothing works without this line
                    update(t);
                }
            }

            //We reset the rule back after letting vanilla handle wakey-wakey
            if (level.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC))
            {
                level.getGameRules().getRule(RULE_DAYLIGHT).set(false, level.getServer());
            }
        }
    }

    public static void update(long worldtime)
    {
        instance.update(Numbers.getCustomTime(worldtime), Numbers.getMultiplier(worldtime));

        instance.dayMinutes = Config.day_length_seconds.get();
        instance.nightMinutes = Config.night_length_seconds.get();
    }
}