package com.unixkitty.timecontrol.network.packet;

import com.unixkitty.timecontrol.TimeControl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.NotNull;

public class GamerulesS2CPacket extends BasePacket
{
    public final boolean vanillaRuleValue;
    public final boolean modRuleValue;

    public GamerulesS2CPacket(@NotNull ServerLevel world)
    {
        this.vanillaRuleValue = world.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        this.modRuleValue = world.getGameRules().getBoolean(TimeControl.DO_DAYLIGHT_CYCLE_TC);
    }

    public GamerulesS2CPacket(FriendlyByteBuf buffer)
    {
        this.vanillaRuleValue = buffer.readBoolean();
        this.modRuleValue = buffer.readBoolean();
    }

    @Override
    public FriendlyByteBuf toBytes(FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(this.vanillaRuleValue);
        buffer.writeBoolean(this.modRuleValue);

        return buffer;
    }
}
