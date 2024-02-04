package com.unixkitty.timecontrol;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.unixkitty.timecontrol.config.Config;
import com.unixkitty.timecontrol.config.json.JsonConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class TimeControlClientCommand
{
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommandManager.literal(TimeControl.MODID + "client");

        registerCommand(Config.ignore_server, command);
        registerCommand(Config.day_length_minutes, command);
        registerCommand(Config.night_length_minutes, command);
        registerCommand(Config.sync_to_system_time, command);
        registerCommand(Config.sync_to_system_time_rate, command);

        //Other commands to do vanilla things to client time
        registerGamerule(TimeControl.DO_DAYLIGHT_CYCLE_TC, command);
        registerGamerule(GameRules.RULE_DAYLIGHT, command);

        command.then(ClientCommandManager.literal("time")
                .then((ClientCommandManager.literal("set")
                        .then(ClientCommandManager.literal("day")
                                .executes((context) -> setTime(context.getSource(), 1000))))
                        .then(ClientCommandManager.literal("noon")
                                .executes((context) -> setTime(context.getSource(), 6000)))
                        .then(ClientCommandManager.literal("night")
                                .executes((context) -> setTime(context.getSource(), 13000)))
                        .then(ClientCommandManager.literal("midnight")
                                .executes((context) -> setTime(context.getSource(), 18000)))
                        .then(ClientCommandManager.argument("time", TimeArgument.time())
                                .executes((context) -> setTime(context.getSource(), IntegerArgumentType.getInteger(context, "time")))))
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("time", TimeArgument.time())
                                .executes((context) -> addTime(context.getSource(), IntegerArgumentType.getInteger(context, "time")))))
                .then((ClientCommandManager.literal("query")
                        .then(ClientCommandManager.literal("daytime")
                                .executes((context) -> queryTime(context.getSource(), getDayTime(context.getSource().getWorld())))))
                        .then(ClientCommandManager.literal("day")
                                .executes((context) -> queryTime(context.getSource(), (int) (context.getSource().getWorld().getDayTime() / 24000L % 2147483647L))))));

        dispatcher.register(command);
    }

    private static int queryTime(FabricClientCommandSource source, int time)
    {
        source.sendFeedback(Component.translatable("commands.time.query", time));

        return time;
    }

    public static int setTime(FabricClientCommandSource source, int time)
    {
        Numbers.setLevelDataWorldtime(source.getWorld(), time);

        int i = getDayTime(source.getWorld());

        source.sendFeedback(Component.translatable("commands.time.set", i));

        return i;
    }

    public static int addTime(FabricClientCommandSource source, int time)
    {
        Numbers.setLevelDataWorldtime(source.getWorld(), source.getWorld().getDayTime() + (long) time);

        int i = getDayTime(source.getWorld());

        source.sendFeedback(Component.translatable("commands.time.set", i));

        return i;
    }

    private static int getDayTime(Level level)
    {
        return (int) (level.getDayTime() % 24000L);
    }

    private static void registerGamerule(GameRules.Key<GameRules.BooleanValue> gameruleKey, LiteralArgumentBuilder<FabricClientCommandSource> command)
    {
        command.then(ClientCommandManager.literal("gamerule")
                .then(ClientCommandManager.literal(gameruleKey.toString())
                        .then(ClientCommandManager.argument(TimeControlCommand.value_string, BoolArgumentType.bool())
                                .executes(context -> setValue(context, gameruleKey, BoolArgumentType.getBool(context, TimeControlCommand.value_string))))
                        .executes(context -> sendFeedback(context, gameruleKey.toString(), getGameRules().getBoolean(gameruleKey), false))));
    }

    private static int setValue(CommandContext<FabricClientCommandSource> context, GameRules.Key<GameRules.BooleanValue> gameruleKey, boolean value)
    {
        if (Config.ignore_server.get())
        {
            getGameRules().getRule(gameruleKey).set(value, null);

            sendFeedback(context, gameruleKey.toString(), getGameRules().getBoolean(gameruleKey), true);

            return 0;
        }
        else
        {
            return ignoreServerError(context);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static GameRules getGameRules()
    {
        return Minecraft.getInstance().level.getGameRules();
    }

    private static void registerCommand(JsonConfig.Value<?> configValue, LiteralArgumentBuilder<FabricClientCommandSource> command)
    {
        ArgumentType<?> argument = null;

        if (configValue.getValueClass().equals(Boolean.class))
        {
            argument = BoolArgumentType.bool();
        }
        else if (configValue instanceof JsonConfig.NumberValue<?> numberValue && numberValue.getValueClass().equals(Integer.class))
        {
            argument = IntegerArgumentType.integer((int) numberValue.getMin(), (int) numberValue.getMax());
        }

        if (argument != null)
        {
            command.then(ClientCommandManager.literal(configValue.getName())
                    .then(ClientCommandManager.argument(TimeControlCommand.value_string, argument)
                            .executes(context -> setValue(context, configValue)))
                    .executes(context -> sendFeedback(context, configValue.getName(), configValue.get(), false)));
        }
    }

    @SuppressWarnings("unchecked")
    private static int setValue(CommandContext<FabricClientCommandSource> context, JsonConfig.Value<?> configValue)
    {
        if (configValue != Config.ignore_server && !Config.ignore_server.get())
        {
            return ignoreServerError(context);
        }

        boolean needSaving = true;

        if (configValue.getValueClass().equals(Boolean.class))
        {
            boolean newValue = BoolArgumentType.getBool(context, TimeControlCommand.value_string);

            ((JsonConfig.Value<Boolean>) configValue).set(newValue);
        }
        else if (configValue.getValueClass().equals(Integer.class))
        {
            int newValue = IntegerArgumentType.getInteger(context, TimeControlCommand.value_string);

            ((JsonConfig.Value<Integer>) configValue).set(newValue);
        }
        else
        {
            needSaving = false;
        }

        if (needSaving)
        {
            Config.save();

            sendFeedback(context, configValue.getName(), configValue.get(), true);
        }

        return 0;
    }

    private static int ignoreServerError(final CommandContext<FabricClientCommandSource> context)
    {
        context.getSource().sendError(Component.translatable("commands.timecontrol.client_not_ignoring_server", Config.ignore_server.getName()));

        return 1;
    }

    private static int sendFeedback(final CommandContext<FabricClientCommandSource> context, String valueName, Object value, boolean valueChanged)
    {
        context.getSource().sendFeedback(Component.translatable("commands.timecontrol." + (valueChanged ? "set" : "query"), valueName, value));

        return 0;
    }
}
