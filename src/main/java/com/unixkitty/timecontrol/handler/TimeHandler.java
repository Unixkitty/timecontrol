package com.unixkitty.timecontrol.handler;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TimeHandler
{
    protected long customtime;
    protected double multiplier;

    public abstract void tick(@NotNull Level level);

    public void update(@Nullable Level level, long customtime, double multiplier)
    {
        this.customtime = customtime;
        this.multiplier = multiplier;
    }
}
