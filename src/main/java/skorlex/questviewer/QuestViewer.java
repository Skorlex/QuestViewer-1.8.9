package skorlex.questviewer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(modid = QuestViewer.MOD_ID, version = QuestViewer.VERSION)
public class QuestViewer {

    public static final String MOD_ID = "questviewer";
    public static final String VERSION = "1.0.0";

    public static KeyBinding checkQuestsKey;

    private int firstTimeDelay = 60;

    private static final Pattern QUEST_COMPLETED_PATTERN = Pattern.compile("(?s).*§r§a(Daily|Weekly|Monthly) Quest: .*? Completed!§r.*");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        QuestViewerConfig.init(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        checkQuestsKey = new KeyBinding("Check Hypixel Quests", Keyboard.KEY_K, "Quests Viewer");
        ClientRegistry.registerKeyBinding(checkQuestsKey);

        ClientCommandHandler.instance.registerCommand(new QuestCommand());

        SoundQueueManager.register();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (checkQuestsKey.isPressed()) {
            Minecraft client = Minecraft.getMinecraft();
            if (client.thePlayer != null) {
                QuestCommand.fetchData(client, client.thePlayer.getName(), "current", false);
            }
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!QuestViewerConfig.getInstance().notificationsEnabled || event.type == 2) return;

        String text = event.message.getFormattedText().trim();
        Matcher matcher = QUEST_COMPLETED_PATTERN.matcher(text);

        if (matcher.matches()) {
            String questType = matcher.group(1);

            if (questType.equals("Daily")) {
                SoundQueueManager.enqueueSound(SoundQueueManager.SoundType.DAILY);
            } else {
                SoundQueueManager.enqueueSound(SoundQueueManager.SoundType.WEEKLY);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft client = Minecraft.getMinecraft();
        if (client.thePlayer != null && QuestViewerConfig.getInstance().firstTimeUser) {
            if (firstTimeDelay > 0) {
                firstTimeDelay--;
            } else {
                QuestViewerConfig.getInstance().firstTimeUser = false;
                QuestViewerConfig.save();

                // QuestViewer is now orange (§6), while the rest remains yellow (§e)
                client.thePlayer.addChatMessage(new ChatComponentText("§e§lUsing §6§lQuestViewer§e§l for the first time? Type §6§l/q help§e§l to view the list of available commands!"));
            }
        }
    }
}