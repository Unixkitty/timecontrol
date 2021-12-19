package com.unixkitty.timecontrol.network.message;

import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.events.TimeEvents;
import com.unixkitty.timecontrol.network.MessageHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

import javax.annotation.Nonnull;

public class GameruleMessageToClient implements IMessage
{
    private boolean isMessageValid;

    private boolean vanillaRuleValue;
    private boolean modRuleValue;

    //For use only by the message handler
    private GameruleMessageToClient()
    {
        this.isMessageValid = false;
    }

    public GameruleMessageToClient(boolean vanillaRuleValue, boolean gameRuleValue)
    {
        this.vanillaRuleValue = vanillaRuleValue;
        this.modRuleValue = gameRuleValue;

        this.isMessageValid = true;
    }

    public boolean getVanillaRule()
    {
        return this.vanillaRuleValue;
    }

    public boolean getModRule()
    {
        return this.modRuleValue;
    }

    @Override
    public boolean isMessageInvalid()
    {
        return !this.isMessageValid;
    }

    public void encode(FriendlyByteBuf buffer)
    {
        if (!isMessageValid) return;

        buffer.writeBoolean(this.vanillaRuleValue);
        buffer.writeBoolean(this.modRuleValue);
    }

    public static GameruleMessageToClient decode(FriendlyByteBuf buffer)
    {
        GameruleMessageToClient message = new GameruleMessageToClient();

        try
        {
            message.vanillaRuleValue = buffer.readBoolean();
            message.modRuleValue = buffer.readBoolean();
        }
        catch (IllegalArgumentException | IndexOutOfBoundsException e)
        {
            TimeControl.log().warn("Exception while reading " + GameruleMessageToClient.class.getSimpleName() + ": " + e);
            return message;
        }

        message.isMessageValid = true;

        return message;
    }

    public static void send(@Nonnull ServerLevel world)
    {
        final boolean vanillaRule = world.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        final boolean modRule = world.getGameRules().getBoolean(TimeEvents.DO_DAYLIGHT_CYCLE_TC);

        MessageHandler.INSTANCE.send(
                PacketDistributor.DIMENSION.with(() -> Level.OVERWORLD),
                new GameruleMessageToClient(vanillaRule, modRule)
        );
    }

    @Override
    public String toString()
    {
        return String.format("%s[vanillaRuleValue=%s,modRuleValue=%s]", getClass().getSimpleName(), this.vanillaRuleValue, this.modRuleValue);
    }
}
