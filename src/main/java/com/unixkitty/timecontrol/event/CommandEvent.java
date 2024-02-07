package com.unixkitty.timecontrol.event;

import com.google.common.base.Throwables;
import com.mojang.brigadier.ParseResults;
import com.unixkitty.timecontrol.handler.ServerTimeHandler;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

public class CommandEvent extends Event
{
    private final ParseResults<CommandSourceStack> parseResults;
    @Nullable
    private Throwable exception;

    public CommandEvent(ParseResults<CommandSourceStack> parseResults)
    {
        this.parseResults = parseResults;
    }

    public ParseResults<CommandSourceStack> getParseResults()
    {
        return this.parseResults;
    }

    @Nullable
    public Throwable getException()
    {
        return this.exception;
    }

    public void setException(@Nullable Throwable exception)
    {
        this.exception = exception;
    }

    @Override
    public boolean execute()
    {
        ServerTimeHandler.onCommand(this);

        return super.execute();
    }

    public static void onCommand(ParseResults<CommandSourceStack> parseResults) throws Exception
    {
        CommandEvent event = new CommandEvent(parseResults);

        if (!event.execute())
        {
            if (event.getException() instanceof Exception e)
            {
                throw e;
            }
            else if (event.getException() != null)
            {
                Throwables.throwIfUnchecked(event.getException());
            }
        }
    }
}
