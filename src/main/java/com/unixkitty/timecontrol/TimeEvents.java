package com.unixkitty.timecontrol;

import com.unixkitty.timecontrol.handler.ITimeHandler;
import com.unixkitty.timecontrol.handler.TimeHandlerClient;
import com.unixkitty.timecontrol.handler.TimeHandlerServer;
import com.unixkitty.timecontrol.network.MessageHandler;
import com.unixkitty.timecontrol.network.PacketGamerule;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandGameRule;
import net.minecraft.command.CommandTime;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber
public class TimeEvents
{
    @SuppressWarnings("WeakerAccess")
    public static final TimeEvents INSTANCE = new TimeEvents();
    public static final String doDaylightCycle = "doDaylightCycle";
    public static final String doDaylightCycle_tc = "doDaylightCycle_tc";

    //private static final Logger log = TimeControl.log;
    private static final ITimeHandler serverTime = new TimeHandlerServer();
    private static final ITimeHandler clientTime = new TimeHandlerClient();

    private TimeEvents()
    {
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        World world = event.getWorld();

        if (world.provider.getDimension() == 0)
        {
            world.getGameRules().setOrCreateGameRule(doDaylightCycle, "false");

            if (!world.getGameRules().hasRule(doDaylightCycle_tc))
            {
                world.getGameRules().setOrCreateGameRule(doDaylightCycle_tc, "true");
            }

            if (!world.isRemote && !Config.sync_to_system())
            {
                serverUpdate(world.getWorldTime());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.player instanceof EntityPlayerMP)
        {
            MessageHandler.INSTANCE.sendTo(new PacketGamerule(event.player.world.getGameRules().getBoolean(doDaylightCycle_tc)), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (
                event.side == Side.CLIENT
                        && event.phase == TickEvent.Phase.START
                        && event.player.world.provider.getDimension() == 0
                        && event.player.world.getGameRules().getBoolean(doDaylightCycle_tc)
                )
        {
            clientTime.tick(event.player.world);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        if (event.world.provider.getDimension() == 0 && event.phase == TickEvent.Phase.START && event.world.getGameRules().getBoolean(doDaylightCycle_tc))
        {
            serverTime.tick(event.world);
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event)
    {
        try
        {
            if (event.getException() == null)
            {
                if (
                        event.getCommand() instanceof CommandGameRule
                                && event.getParameters().length >= 1
                                && (event.getParameters()[0].equals(doDaylightCycle) || event.getParameters()[0].equals(doDaylightCycle_tc))
                                && (event.getSender().getServer() != null)
                        )
                {
                    event.getParameters()[0] = doDaylightCycle_tc;

                    new CommandGameRule().execute(event.getSender().getServer(), event.getSender(), event.getParameters());

                    if (event.getParameters().length >= 2)
                    {
                        MessageHandler.INSTANCE.sendToAll(new PacketGamerule(CommandBase.parseBoolean(event.getParameters()[1])));
                    }

                    event.setCanceled(true);
                }
                else if (event.getCommand() instanceof CommandTime && event.getParameters().length == 2)
                {
                    String[] args = event.getParameters();

                    if (args[0].equals("set") || args[0].equals("add"))
                    {
                        String arg = args[1];
                        long time;

                        if (args[0].equals("add"))
                        {
                            time = CommandBase.parseLong(arg);

                            time = event.getSender().getServer().getWorld(0).getWorldTime() + time;
                        }
                        else
                        {
                            switch (arg)
                            {
                                case "day":
                                    time = 1000L;
                                    break;
                                case "night":
                                    time = 13000L;
                                    break;
                                default:
                                    time = CommandBase.parseLong(arg);
                                    break;
                            }
                        }

                        if (Config.sync_to_system())
                        {
                            event.getSender().sendMessage(new TextComponentString(TextFormatting.RED + "Disable system time synchronization to " + args[0] + " time!"));
                            event.setCanceled(true);
                        }
                        else
                        {
                            serverUpdate(time);
                        }
                    }
                }
            }
        }
        catch (CommandException e)
        {
            event.setException(e);
        }
    }

    public void clientUpdate(long customtime, double multiplier)
    {
        clientTime.update(customtime, multiplier);
    }

    private void serverUpdate(long worldtime)
    {
        serverTime.update(Numbers.customtime(worldtime), Numbers.multiplier(worldtime));
    }
}
