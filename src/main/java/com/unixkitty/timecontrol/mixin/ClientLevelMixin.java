package com.unixkitty.timecontrol.mixin;

import com.unixkitty.timecontrol.config.Config;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin
{
    @Inject(at = @At("HEAD"), method = "setDayTime(J)V", cancellable = true)
    public void setDayTime(long time, CallbackInfo ci)
    {
        if (Config.ignore_server.get())
        {
            ci.cancel();
        }
    }
}
