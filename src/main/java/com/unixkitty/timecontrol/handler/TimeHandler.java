package com.unixkitty.timecontrol.handler;

import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public abstract class TimeHandler
{
    protected long customtime;
    protected double multiplier;

    public abstract void tick(@Nonnull Level level);

    public void update(long customtime, double multiplier)
    {
        this.customtime = customtime;
        this.multiplier = multiplier;
    }
}
