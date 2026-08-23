package skorlex.questviewer;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.LinkedList;
import java.util.Queue;

public class SoundQueueManager {

    private static final Queue<SoundType> QUEUE = new LinkedList<>();
    private static int tickCooldown = 0;

    public enum SoundType {
        DAILY(12),  // 600ms (12 ticks)
        WEEKLY(24); // 1200ms (24 ticks)

        public final int cooldown;

        SoundType(int cooldown) {
            this.cooldown = cooldown;
        }
    }

    public static void enqueueSound(SoundType type) {
        QUEUE.add(type);
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new SoundQueueManager());
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Clears the queue ONLY when you fully leave the server/game
        if (!QUEUE.isEmpty()) {
            QUEUE.clear();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft client = Minecraft.getMinecraft();

        // Pause tick processing while transitioning between sub-servers/lobbies
        if (client.thePlayer == null) return;

        if (tickCooldown > 0) {
            tickCooldown--;
            return;
        }

        if (!QUEUE.isEmpty()) {
            SoundType nextSound = QUEUE.poll();
            playSound(client, nextSound);
            tickCooldown = nextSound.cooldown;
        }
    }

    private static void playSound(Minecraft client, SoundType type) {
        String soundEvent = (type == SoundType.WEEKLY) ? "chime_weekly" : "chime_daily";
        float pitch = (type == SoundType.WEEKLY)
                ? QuestViewerConfig.getInstance().weeklyPitch
                : QuestViewerConfig.getInstance().dailyPitch;

        if (client.thePlayer != null) {
            client.thePlayer.playSound("questviewer:" + soundEvent, 100.0F, pitch);
        }
    }
}