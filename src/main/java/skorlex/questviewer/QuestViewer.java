package skorlex.questviewer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = QuestViewer.MOD_ID, version = QuestViewer.VERSION)
public class QuestViewer {

    public static final String MOD_ID = "questviewer";
    public static final String VERSION = "1.0.0";

    public static KeyBinding checkQuestsKey;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Grab the Forge config directory (usually .minecraft/config) and initialize our file
        QuestViewerConfig.init(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        checkQuestsKey = new KeyBinding("Check Hypixel Quests", Keyboard.KEY_K, "Quests Viewer");
        ClientRegistry.registerKeyBinding(checkQuestsKey);

        ClientCommandHandler.instance.registerCommand(new QuestCommand());

        // Register this class to the main Forge Event Bus to receive chat events
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
        // Ignore the event if notifications are disabled, or if it is an action bar overlay (type 2)
        if (!QuestViewerConfig.getInstance().notificationsEnabled || event.type == 2) return;

        // Grab the exact formatted text (including color codes) to prevent spoofing or false positives
        String text = event.message.getFormattedText().trim();

        // (?s) enables DOTALL so it matches across the multi-line reward text
        if (text.matches("(?s).*§r§a(Daily|Weekly|Monthly) Quest: .*? Completed!§r.*")) {
            Minecraft client = Minecraft.getMinecraft();
            QuestCommand.playTestSound(client);
        }
    }
}