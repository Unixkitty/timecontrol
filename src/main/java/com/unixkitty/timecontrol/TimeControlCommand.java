package com.unixkitty.timecontrol;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.unixkitty.timecontrol.config.Config;
import com.unixkitty.timecontrol.config.json.JsonConfig;
import com.unixkitty.timecontrol.network.ModNetworkDispatcher;
import com.unixkitty.timecontrol.network.packet.ConfigS2CPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TimeControlCommand
{
    static final String value_string = "value";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(TimeControl.MODID)
                .requires(source -> source.hasPermission(2));

        registerCommand(Config.day_length_minutes, command);
        registerCommand(Config.night_length_minutes, command);
        registerCommand(Config.sync_to_system_time, command);
        registerCommand(Config.sync_to_system_time_rate, command);

        dispatcher.register(command);
    }

    private static void registerCommand(JsonConfig.Value<?> configValue, LiteralArgumentBuilder<CommandSourceStack> command)
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
            command.then(Commands.literal(configValue.getName())
                    .then(Commands.argument(value_string, argument)
                            .executes(context -> setValue(context, configValue)))
                    .executes(context -> sendFeedback(context, configValue, false)));
        }
    }

    @SuppressWarnings("unchecked")
    private static int setValue(CommandContext<CommandSourceStack> context, JsonConfig.Value<?> configValue)
    {
        boolean needSaving = true;

        if (configValue.getValueClass().equals(Boolean.class))
        {
            boolean newValue = BoolArgumentType.getBool(context, value_string);

            ((JsonConfig.Value<Boolean>) configValue).set(newValue);
        }
        else if (configValue.getValueClass().equals(Integer.class))
        {
            int newValue = IntegerArgumentType.getInteger(context, value_string);

            ((JsonConfig.Value<Integer>) configValue).set(newValue);
        }
        else
        {
            needSaving = false;
        }

        if (needSaving)
        {
            Config.save();

            ModNetworkDispatcher.send(context.getSource().getLevel(), new ConfigS2CPacket());

            sendFeedback(context, configValue, true);
        }

        return 0;
    }

    private static int sendFeedback(final CommandContext<CommandSourceStack> context, final JsonConfig.Value<?> configValue, boolean allowLogging)
    {
        context.getSource().sendSuccess(() -> Component.translatable("commands.timecontrol." + (allowLogging ? "set" : "query"), configValue.getName(), configValue.get()), allowLogging);

        return 0;
    }
}
