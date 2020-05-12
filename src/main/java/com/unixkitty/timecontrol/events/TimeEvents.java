package com.unixkitty.timecontrol.events;

import com.unixkitty.timecontrol.Config;
import com.unixkitty.timecontrol.Numbers;
import com.unixkitty.timecontrol.TimeControl;
import com.unixkitty.timecontrol.handler.ClientTimeHandler;
import com.unixkitty.timecontrol.handler.ITimeHandler;
import com.unixkitty.timecontrol.handler.ServerTimeHandler;
import com.unixkitty.timecontrol.network.MessageHandler;
import com.unixkitty.timecontrol.network.message.GameruleMessageToClient;
import com.unixkitty.timecontrol.network.message.IMessage;
import com.unixkitty.timecontrol.network.message.TimeMessageToClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.SleepFinishedTimeEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = TimeControl.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TimeEvents
{
    public static GameRules.RuleKey<GameRules.BooleanValue> DO_DAYLIGHT_CYCLE_TC = null;

    private static final ITimeHandler SERVER = new ServerTimeHandler();
    private static final ITimeHandler CLIENT = new ClientTimeHandler();

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event)
    {
        MessageHandler.init();

        initGameRule();
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event)
    {
        if (DO_DAYLIGHT_CYCLE_TC == null) return;

        World world = event.getWorld().getWorld();

        if (world.getDimension().getType() == DimensionType.OVERWORLD)
        {
            MinecraftServer server = null;

            if (!world.isRemote() && world instanceof ServerWorld)
            {
                server = ((ServerWorld) world).getServer();
            }

            world.getGameRules().get(DO_DAYLIGHT_CYCLE).set(false, server);
            //Is this even necessary?
            world.getGameRules().get(DO_DAYLIGHT_CYCLE_TC).set(world.getGameRules().getBoolean(DO_DAYLIGHT_CYCLE_TC), server);

            if (!world.isRemote() && !Config.sync_to_system_time.get())
            {
                updateServer(world.getDayTime());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (
                event.side == LogicalSide.CLIENT
                        && event.phase == TickEvent.Phase.START
                        && event.player.world.dimension.getType() == DimensionType.OVERWORLD
        )
        {
            CLIENT.tick(event.player.world);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event)
    {
        if (
                event.side == LogicalSide.SERVER
                        && event.phase == TickEvent.Phase.START
                        && event.world.dimension.getType() == DimensionType.OVERWORLD
        )
        {
            SERVER.tick(event.world);
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event)
    {
        //TODO handle /gamerule and /time commands
        /*
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
         */
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event)
    {
        //TODO handle sleep
        TimeControl.log().debug("Sleep finished!");
    }

    public static void updateClient(IMessage updateMessage)
    {
        if (updateMessage instanceof TimeMessageToClient)
        {
            CLIENT.update(((TimeMessageToClient) updateMessage).getCustomtime(), ((TimeMessageToClient) updateMessage).getMultiplier());
        }

        if (updateMessage instanceof GameruleMessageToClient)
        {
            ClientWorld world = Minecraft.getInstance().world;

            if (world != null)
            {
                world.getGameRules().get(DO_DAYLIGHT_CYCLE).set(((GameruleMessageToClient) updateMessage).getVanillaRule(), null);
                world.getGameRules().get(DO_DAYLIGHT_CYCLE_TC).set(((GameruleMessageToClient) updateMessage).getModRule(), null);
            }
        }
    }

    public static void updateServer(long worldtime)
    {
        SERVER.update(Numbers.customtime(worldtime), Numbers.multiplier(worldtime));
    }

    private static void initGameRule()
    {
        Method createBoolean = ObfuscationReflectionHelper.findMethod(GameRules.BooleanValue.class, "func_223568_b", boolean.class);
        createBoolean.setAccessible(true);

        DeferredWorkQueue.runLater(() ->
        {
            try
            {
                Object boolTrue = createBoolean.invoke(GameRules.BooleanValue.class, true);

                DO_DAYLIGHT_CYCLE_TC = GameRules.register("doDaylightCycle_tc", (GameRules.RuleType<GameRules.BooleanValue>) boolTrue);
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                TimeControl.log().error("Failed to create gamerule!");

                e.printStackTrace();
            }
        });
    }
}