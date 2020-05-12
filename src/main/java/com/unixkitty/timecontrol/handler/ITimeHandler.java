package com.unixkitty.timecontrol.handler;

import net.minecraft.world.World;

public interface ITimeHandler
{
    void tick(World world);

    void update(long customtime, double multiplier);
}
