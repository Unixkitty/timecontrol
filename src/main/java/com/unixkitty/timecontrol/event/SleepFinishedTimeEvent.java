package com.unixkitty.timecontrol.event;

import com.unixkitty.timecontrol.handler.ServerTimeHandler;
import net.minecraft.server.level.ServerLevel;

public class SleepFinishedTimeEvent extends Event
{
    private final ServerLevel level;
    private final long minTime;

    private long newTime;

    public SleepFinishedTimeEvent(ServerLevel level, long newTime, long minTime)
    {
        this.level = level;
        this.newTime = newTime;
        this.minTime = minTime;
    }

    public long getNewTime()
    {
        return this.newTime;
    }

    public boolean setTimeAddition(long time)
    {
        if (this.minTime > time)
        {
            return false;
        }

        this.newTime = time;

        return true;
    }

    public ServerLevel getLevel()
    {
        return this.level;
    }

    @Override
    public boolean isCancelable()
    {
        return !super.isCancelable();
    }

    @Override
    public boolean execute()
    {
        ServerTimeHandler.onSleepFinished(this);

        return super.execute();
    }

    public static long onSleepFinished(ServerLevel level, long newTime, long minTime)
    {
        SleepFinishedTimeEvent event = new SleepFinishedTimeEvent(level, newTime, minTime);

        event.execute();

        return event.getNewTime();
    }
}
