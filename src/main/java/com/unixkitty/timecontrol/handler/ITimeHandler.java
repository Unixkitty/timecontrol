package com.unixkitty.timecontrol.handler;

import net.minecraft.world.level.Level;

public interface ITimeHandler
{
    void tick(Level world);

    void update(long customtime, double multiplier);
}
