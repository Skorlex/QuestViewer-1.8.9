package skorlex.questviewer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "questviewer", name = "QuestViewer", version = "1.0.0", clientSideOnly = true)
public class QuestViewer {

    public static KeyBinding checkQuestsKey;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Updated to use clean display strings instead of raw localization keys
        checkQuestsKey = new KeyBinding("Check Hypixel Quests", Keyboard.KEY_K, "Quests Viewer");
        ClientRegistry.registerKeyBinding(checkQuestsKey);
        FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new QuestCommand());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (checkQuestsKey.isPressed()) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null) {
                    String selfName = mc.thePlayer.getName();
                    QuestCommand.fetchData(mc, selfName, "current", false);
                }
            }
        }
    }
}