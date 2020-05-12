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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.PacketDistributor;

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

            world.getWorldInfo().getGameRulesInstance().get(DO_DAYLIGHT_CYCLE).set(false, server);
            world.getWorldInfo().getGameRulesInstance().get(DO_DAYLIGHT_CYCLE_TC).set(true, server);

            if (!world.isRemote() && !Config.sync_to_system_time.get())
            {
                updateServer(world.getDayTime());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getPlayer() instanceof ServerPlayerEntity)
        {
            MessageHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                    new GameruleMessageToClient(((ServerPlayerEntity) event.getPlayer()).getServerWorld().getGameRules().getBoolean(DO_DAYLIGHT_CYCLE_TC))
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (
                event.side == LogicalSide.CLIENT
                        && event.phase == TickEvent.Phase.START
                        && event.player.world.dimension.getType() == DimensionType.OVERWORLD
                        && event.player.world.getGameRules().getBoolean(DO_DAYLIGHT_CYCLE_TC)
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
                        && event.world.getGameRules().getBoolean(DO_DAYLIGHT_CYCLE_TC)
        )
        {
            SERVER.tick(event.world);
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event)
    {
        //TODO handle /gamerule and /time commands
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
                world.getGameRules().get(DO_DAYLIGHT_CYCLE_TC).set(((GameruleMessageToClient) updateMessage).get(), null);
            }
        }
    }

    private static void updateServer(long worldtime)
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

                DO_DAYLIGHT_CYCLE_TC = GameRules.register("doAttackCooldown", (GameRules.RuleType<GameRules.BooleanValue>) boolTrue);
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                TimeControl.log().error("Failed to create gamerule!");

                e.printStackTrace();
            }
        });
    }
}