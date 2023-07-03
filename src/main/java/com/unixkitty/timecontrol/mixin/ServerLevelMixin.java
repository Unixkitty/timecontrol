package com.unixkitty.timecontrol.mixin;

import com.unixkitty.timecontrol.event.SleepFinishedTimeEvent;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerLevel.class)
public class ServerLevelMixin
{
    @ModifyArg(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    public long onSleepFinished(long time)
    {
        ServerLevel level = (ServerLevel) (Object) this;

        return SleepFinishedTimeEvent.onSleepFinished(level, time, level.getDayTime());
    }
}
