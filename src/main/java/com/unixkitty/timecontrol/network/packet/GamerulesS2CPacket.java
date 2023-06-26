package com.unixkitty.timecontrol.network.packet;

import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.handler.ClientTimeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class GamerulesS2CPacket extends BasePacket
{
    public final boolean vanillaRuleValue;
    public final boolean modRuleValue;

    public GamerulesS2CPacket(@Nonnull ServerLevel world)
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
    public void toBytes(FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(this.vanillaRuleValue);
        buffer.writeBoolean(this.modRuleValue);
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> ClientTimeHandler.handlePacket(this));

        context.setPacketHandled(true);

        return true;
    }
}
