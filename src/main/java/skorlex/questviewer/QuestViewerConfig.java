package skorlex.questviewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class QuestViewerConfig {

    private static File configFile;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // The newly decoupled config variables
    public boolean notificationsEnabled = true;
    public float dailyPitch = 1.2F;
    public float weeklyPitch = 1.2F;

    private static QuestViewerConfig instance;

    public static QuestViewerConfig getInstance() {
        if (instance == null) {
            instance = new QuestViewerConfig();
        }
        return instance;
    }

    public static void init(File configDir) {
        configFile = new File(configDir, "questviewer.json");
        load();
    }

    public static void load() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                instance = GSON.fromJson(reader, QuestViewerConfig.class);
            } catch (Exception e) {
                System.err.println("[QuestViewer] Could not load config file, falling back to defaults.");
                e.printStackTrace();
                instance = new QuestViewerConfig();
            }
        } else {
            instance = new QuestViewerConfig();
            save();
        }
    }

    public static void save() {
        if (configFile == null) return;
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(getInstance(), writer);
        } catch (IOException e) {
            System.err.println("[QuestViewer] Could not save config file.");
            e.printStackTrace();
        }
    }
}