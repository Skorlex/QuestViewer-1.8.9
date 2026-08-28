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
    public static final String VERSION = "1.1.0";

    public static KeyBinding checkQuestsKey;
    public static KeyBinding checkWeeklyQuestsKey;

    private int firstTimeDelay = 60;

    private static final Pattern QUEST_COMPLETED_PATTERN = Pattern.compile("(?s).*§r§a(Daily|Weekly|Monthly) Quest: .*? Completed!§r.*");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        QuestViewerConfig.init(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        checkQuestsKey = new KeyBinding("key.questviewer.check_daily_quests", Keyboard.KEY_K, "key.category.questviewer.quests");
        checkWeeklyQuestsKey = new KeyBinding("key.questviewer.check_weekly_quests", Keyboard.KEY_J, "key.category.questviewer.quests");

        ClientRegistry.registerKeyBinding(checkQuestsKey);
        ClientRegistry.registerKeyBinding(checkWeeklyQuestsKey);

        ClientCommandHandler.instance.registerCommand(new QuestCommand());

        SoundQueueManager.register();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.thePlayer != null) {
            if (checkQuestsKey.isPressed()) {
                QuestCommand.fetchData(client, client.thePlayer.getName(), "current", "daily");
            }
            if (checkWeeklyQuestsKey.isPressed()) {
                QuestCommand.fetchData(client, client.thePlayer.getName(), "current", "weekly");
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

                client.thePlayer.addChatMessage(new ChatComponentText("§e§lUsing §6§lQuestViewer§e§l for the first time? Type §6§l/q help§e§l to view the list of available commands!"));
            }
        }
    }
}