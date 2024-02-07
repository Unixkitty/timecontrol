package com.unixkitty.timecontrol.mixin;

import com.mojang.brigadier.ParseResults;
import com.unixkitty.timecontrol.event.CommandEvent;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Commands.class)
public class CommandsMixin
{
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/util/function/Supplier;)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void onCommand(ParseResults<CommandSourceStack> parseResults, String string, CallbackInfo ci, CommandSourceStack commandSourceStack)
    {
        try
        {
            CommandEvent.onCommand(parseResults);
        }
        catch (Exception e)
        {
            commandSourceStack.sendFailure(Component.literal(Util.describeError(e)));
            LOGGER.error("'/{}' threw an exception", string, e);
            ci.cancel();
        }
    }
}
