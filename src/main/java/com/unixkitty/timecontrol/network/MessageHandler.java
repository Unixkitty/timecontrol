package com.unixkitty.timecontrol.network;

import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.events.TimeEvents;
import com.unixkitty.timecontrol.network.message.GameruleMessageToClient;
import com.unixkitty.timecontrol.network.message.IMessage;
import com.unixkitty.timecontrol.network.message.TimeMessageToClient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT;

public class MessageHandler
{
    public static final byte TIME_MESSAGE_ID = 1;
    public static final byte GAMERULE_MESSAGE_ID = 2;

    public static SimpleChannel INSTANCE;
    public static final ResourceLocation resourceLocation = new ResourceLocation(TimeControl.MODID, "message_channel");
    public static final String MESSAGE_PROTOCOL_VERSION = "1";

    private static boolean initialized = false;

    public static void init()
    {
        if (initialized) return;
        initialized = true;

        INSTANCE = NetworkRegistry.newSimpleChannel(resourceLocation, () ->
                        MESSAGE_PROTOCOL_VERSION,
                MessageHandler::shouldClientAccept,
                MessageHandler::shouldServerAccept
        );

        INSTANCE.registerMessage(
                TIME_MESSAGE_ID,
                TimeMessageToClient.class,
                TimeMessageToClient::encode,
                TimeMessageToClient::decode,
                MessageHandler::handleClientMessage,
                Optional.of(PLAY_TO_CLIENT)
        );
        INSTANCE.registerMessage(
                GAMERULE_MESSAGE_ID,
                GameruleMessageToClient.class,
                GameruleMessageToClient::encode,
                GameruleMessageToClient::decode,
                MessageHandler::handleClientMessage,
                Optional.of(PLAY_TO_CLIENT)
        );
    }

    public static boolean shouldClientAccept(String protocolVersion)
    {
        return MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }

    public static void handleClientMessage(final IMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        LogicalSide sideReceived = context.getDirection().getReceptionSide();

        context.setPacketHandled(true);

        if (sideReceived != LogicalSide.CLIENT || message.isMessageInvalid())
        {
            TimeControl.log().warn(message.getClass().getSimpleName() + " was invalid: side " + sideReceived + ", message: " + message.toString());
            return;
        }

        context.enqueueWork(() -> TimeEvents.updateClient(message));
    }

    public static boolean shouldServerAccept(String protocolVersion)
    {
        return MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
