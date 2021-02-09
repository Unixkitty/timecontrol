package com.unixkitty.timecontrol;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class CommandTimeControl
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(Commands.literal(TimeControl.MODID).requires(commandSource -> commandSource.hasPermissionLevel(2)).then(
                Commands.literal("set")/*.then(
                        Commands.literal(Config.SYNC_TO_SYSTEM_TIME).then(
                                Commands.argument("boolean", BoolArgumentType.bool()).executes(context -> setSystemSync(context, BoolArgumentType.getBool(context, "boolean")))
                        ))*/.then(
                        Commands.literal(Config.NIGHT_LENGTH_MINUTES).then(
                                Commands.argument("value", IntegerArgumentType.integer(1, Config.LENGTH_LIMIT)).executes(
                                        context -> setLengthMinutes(context, false)
                                )
                        )).then(
                        Commands.literal(Config.DAY_LENGTH_MINUTES).then(
                                Commands.argument("value", IntegerArgumentType.integer(1, Config.LENGTH_LIMIT)).executes(
                                        context -> setLengthMinutes(context, true)
                                )
                        ))
                ).then(
                Commands.literal("get").then(
                        Commands.literal(Config.SYNC_TO_SYSTEM_TIME).executes(
                                context -> sendFeedback(context.getSource(), Config.SYNC_TO_SYSTEM_TIME, Config.sync_to_system_time.get(), false)
                        )).then(
                        Commands.literal(Config.NIGHT_LENGTH_MINUTES).executes(
                                context -> sendFeedback(context.getSource(), Config.NIGHT_LENGTH_MINUTES, Config.night_length_minutes.get(), false)
                        )).then(
                        Commands.literal(Config.DAY_LENGTH_MINUTES).executes(
                                context -> sendFeedback(context.getSource(), Config.DAY_LENGTH_MINUTES, Config.day_length_minutes.get(), false)
                        )
                )
                )
        );
    }

    private static int setLengthMinutes(CommandContext<CommandSource> context, boolean day)
    {
        final int value = IntegerArgumentType.getInteger(context, "value");

        if (day)
        {
            Config.day_length_minutes.set(value);
            sendFeedback(context.getSource(), Config.DAY_LENGTH_MINUTES, value, true);
        }
        else
        {
            Config.night_length_minutes.set(value);
            sendFeedback(context.getSource(), Config.NIGHT_LENGTH_MINUTES, value, true);
        }

        return 0;
    }

    /*private static int setSystemSync(CommandContext<CommandSource> context, boolean value)
    {
        Config.sync_to_system_time.set(value);

        return sendFeedback(context.getSource(), Config.SYNC_TO_SYSTEM_TIME, value, true);
    }*/

    private static int sendFeedback(final CommandSource source, final String valueName, final Object value, boolean allowLogging)
    {
        source.sendFeedback(new StringTextComponent(valueName + " = " + value), allowLogging);

        return 0;
    }
}
