package skorlex.questviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "q";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("quest", "quests");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/q help";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "daily", "weekly", "leaderboard", "stats", "summary", "sum", "site", "games", "legacy", "notification", "help");
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        Minecraft client = Minecraft.getMinecraft();
        if (client.thePlayer == null) return;

        String selfName = client.thePlayer.getName();

        if (args.length == 0) {
            printHelp(client);
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "help":
                case "h":
                    printHelp(client);
                    break;
                case "games":
                    fetchGames(client);
                    break;
                case "lb":
                case "leaderboard":
                    fetchLeaderboard(client, 1);
                    break;
                case "stats":
                case "s":
                    fetchStats(client, selfName);
                    break;
                case "sum":
                case "summary":
                    fetchSummary(client, selfName);
                    break;
                case "weekly":
                case "w":
                    fetchData(client, selfName, "current", true);
                    break;
                case "daily":
                case "d":
                    fetchData(client, selfName, "current", false);
                    break;
                case "site":
                    printSite(client, selfName);
                    break;
                case "notification":
                case "notifications":
                case "notify":
                case "n":
                    client.thePlayer.addChatMessage(new ChatComponentText("§cPlease specify a sound to test: §e/q n daily §cor §e/q n weekly"));
                    break;
                case "summer_albert":
                case "albert":
                case "bert":
                    printAlbert(client);
                    break;
                default:
                    client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not register argument: " + args[0]));
                    client.thePlayer.addChatMessage(new ChatComponentText("§cType '/q help' for a list of commands"));
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "notification":
                case "notifications":
                case "notify":
                case "n":
                    if (args[1].equalsIgnoreCase("daily") || args[1].equalsIgnoreCase("d")) {
                        playNotificationSound(client, "chime_daily", QuestViewerConfig.getInstance().dailyPitch);
                    } else if (args[1].equalsIgnoreCase("weekly") || args[1].equalsIgnoreCase("w")) {
                        playNotificationSound(client, "chime_weekly", QuestViewerConfig.getInstance().weeklyPitch);
                    } else {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cUnknown sub-command. Try §e/q n daily §cor §e/q n weekly"));
                    }
                    break;
                case "lb":
                case "leaderboard":
                    int page = 1;
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                    }

                    if (page > 10) {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cYou cannot go higher than page 10."));
                        break;
                    } else if (page < 1) {
                        page = 1;
                    }

                    fetchLeaderboard(client, page);
                    break;
                case "site":
                    printSite(client, args[1]);
                    break;
                case "stats":
                case "s":
                    fetchStats(client, args[1]);
                    break;
                case "sum":
                case "summary":
                    fetchSummary(client, args[1]);
                    break;
                case "weekly":
                case "w":
                    fetchData(client, selfName, args[1], true);
                    break;
                case "daily":
                case "d":
                    fetchData(client, selfName, args[1], false);
                    break;
                case "legacy":
                    fetchData(client, args[1], "legacy", false);
                    break;
                default:
                    client.thePlayer.addChatMessage(new ChatComponentText("§cCould not register argument: " + args[0]));
                    client.thePlayer.addChatMessage(new ChatComponentText("§cType '/q help' for a list of commands"));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "weekly":
                case "w":
                    fetchData(client, args[2], args[1], true);
                    break;
                case "daily":
                case "d":
                    fetchData(client, args[2], args[1], false);
                    break;
            }
        } else {
            client.thePlayer.addChatMessage(new ChatComponentText("§cCould not process command"));
            client.thePlayer.addChatMessage(new ChatComponentText("§cType '/q help' for a list of commands"));
        }
    }

    public static void playNotificationSound(Minecraft client, String soundEvent, float pitch) {
        if (client.thePlayer != null) {
            client.thePlayer.playSound("questviewer:" + soundEvent, 100.0F, pitch);
        }
    }

    private static void getAsync(String urlString, Consumer<String> onSuccess, Consumer<String> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "QuestViewer");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                onSuccess.accept(response.toString());
            } catch (Exception e) {
                onError.accept(e.getMessage());
            }
        });
    }

    private static void printHelp(Minecraft client) {
        client.thePlayer.addChatMessage(new ChatComponentText(""));
        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
        client.thePlayer.addChatMessage(new ChatComponentText(""));
        client.thePlayer.addChatMessage(new ChatComponentText("§lHelp and Commands"));
        client.thePlayer.addChatMessage(new ChatComponentText(""));
        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q daily §7- Your daily quests for game you are playing"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q weekly §7- Your weekly quests for game you are playing"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q daily [game] {ign} §7- Your daily quests for specified game"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q weekly [game] {ign} §7- Your weekly quests for specified game"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q summary §7- View quests completed summary (- /q sum [ign])"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q leaderboard [1-10] §7- View the top 100 quests completed (- /q lb)"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q stats §7- View your general Hypixel stats (- /q s [ign])"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q n daily §7- Test Daily sound (- /q n d)"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q n weekly §7- Test Weekly sound (- /q n w)"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q site §7- Link to 25Karma quest page (- /q site [ign])"));
        client.thePlayer.addChatMessage(new ChatComponentText("§e/q games §7- Lists gamemode aliases"));
        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
    }

    private static void printSite(Minecraft client, String ign) {
        getAsync("https://playerdb.co/api/player/minecraft/" + ign, response -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer == null) return;
                try {
                    JsonObject data = new JsonParser().parse(response).getAsJsonObject();
                    if (data.has("success") && data.get("success").getAsBoolean()) {
                        String uuid = data.getAsJsonObject("data").getAsJsonObject("player").get("raw_id").getAsString();
                        String url = "https://25karma.xyz/quests/" + uuid;

                        ChatComponentText linkComponent = new ChatComponentText("25Karma");
                        linkComponent.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
                        linkComponent.getChatStyle().setBold(true);
                        linkComponent.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                        linkComponent.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(url)));

                        ChatComponentText message = new ChatComponentText("Go to ");
                        message.appendSibling(linkComponent);

                        client.thePlayer.addChatMessage(new ChatComponentText("§m-------------------"));
                        client.thePlayer.addChatMessage(new ChatComponentText(""));
                        client.thePlayer.addChatMessage(message);
                        client.thePlayer.addChatMessage(new ChatComponentText(""));
                        client.thePlayer.addChatMessage(new ChatComponentText("§m-------------------"));
                    } else {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not find player UUID."));
                    }
                } catch (Exception e) {
                    client.thePlayer.addChatMessage(new ChatComponentText("§cError connecting to PlayerDB."));
                }
            });
        }, error -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer != null) client.thePlayer.addChatMessage(new ChatComponentText("§cError connecting to PlayerDB."));
            });
        });
    }

    private static void printAlbert(Minecraft client) {
        String url = "https://sites.google.com/view/summeralbert/home";
        ChatComponentText linkComponent = new ChatComponentText("Albert's Achives");
        linkComponent.getChatStyle().setColor(EnumChatFormatting.AQUA);
        linkComponent.getChatStyle().setBold(true);
        linkComponent.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        linkComponent.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(url)));

        ChatComponentText message = new ChatComponentText("Go to ");
        message.appendSibling(linkComponent);

        client.thePlayer.addChatMessage(new ChatComponentText("§m-------------------"));
        client.thePlayer.addChatMessage(new ChatComponentText(""));
        client.thePlayer.addChatMessage(message);
        client.thePlayer.addChatMessage(new ChatComponentText(""));
        client.thePlayer.addChatMessage(new ChatComponentText("§m-------------------"));
    }

    private static void fetchGames(Minecraft client) {
        getAsync("https://questviewer-proxy.skorlex.workers.dev/api/misc/gameAliases/", response -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer == null) return;
                try {
                    JsonObject data = new JsonParser().parse(response).getAsJsonObject();
                    if (data.has("success") && data.get("success").getAsBoolean()) {
                        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                        client.thePlayer.addChatMessage(new ChatComponentText(""));
                        client.thePlayer.addChatMessage(new ChatComponentText("§lGame Aliases"));
                        client.thePlayer.addChatMessage(new ChatComponentText(""));
                        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                        JsonArray array = data.get("data").getAsJsonArray();
                        for (JsonElement game : array) {
                            JsonObject gameObj = game.getAsJsonObject();
                            String name = gameObj.get("name").getAsString();
                            StringBuilder aliasList = new StringBuilder();
                            JsonArray aliases = gameObj.get("aliases").getAsJsonArray();
                            for (int i = 0; i < aliases.size(); i++) {
                                if (i > 0) aliasList.append(", ");
                                aliasList.append(aliases.get(i).getAsString());
                            }
                            client.thePlayer.addChatMessage(new ChatComponentText("§f" + name));
                            client.thePlayer.addChatMessage(new ChatComponentText("§7- " + aliasList));
                        }
                        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                    } else if (data.has("cause")) {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cError: " + data.get("cause").getAsString()));
                    } else {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cAn error has occurred"));
                    }
                } catch (Exception e) {
                    client.thePlayer.addChatMessage(new ChatComponentText("§cAn error has occurred parsing games."));
                }
            });
        }, error -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer != null) client.thePlayer.addChatMessage(new ChatComponentText("§cFailed to fetch games: " + error));
            });
        });
    }

    private static void fetchLeaderboard(Minecraft client, int page) {
        getAsync("https://plancke.io/hypixel/leaderboards/raw.php?type=player.general.quests", response -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer == null) return;
                try {
                    JsonObject root = new JsonParser().parse(response).getAsJsonObject();
                    String html = root.get("result").getAsString();

                    client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                    client.thePlayer.addChatMessage(new ChatComponentText(""));

                    ChatComponentText header = new ChatComponentText("");

                    if (page > 1) {
                        ChatComponentText prev = new ChatComponentText("§b§l<< ");
                        prev.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("§bClick to view page " + (page - 1))));
                        prev.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/q lb " + (page - 1)));
                        header.appendSibling(prev);
                    }

                    header.appendSibling(new ChatComponentText("§f§lTop " + (page * 10) + " Quests Completed"));

                    if (page < 10) {
                        ChatComponentText next = new ChatComponentText(" §b§l>>");
                        next.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("§bClick to view page " + (page + 1))));
                        next.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/q lb " + (page + 1)));
                        header.appendSibling(next);
                    }

                    client.thePlayer.addChatMessage(header);
                    client.thePlayer.addChatMessage(new ChatComponentText(""));
                    client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));

                    Pattern rowPattern = Pattern.compile("(?s)<tr>\\s*<td>(\\d+)</td>\\s*<td>(.*?)</td>\\s*<td>([\\d,]+)</td>");
                    Matcher matcher = rowPattern.matcher(html);

                    int currentRank = 0;
                    int startIndex = (page - 1) * 10;
                    int endIndex = page * 10;
                    boolean foundAny = false;

                    while (matcher.find()) {
                        currentRank++;
                        if (currentRank <= startIndex) continue;
                        if (currentRank > endIndex) break;

                        foundAny = true;
                        String rank = matcher.group(1);
                        String rawPlayerCell = matcher.group(2);
                        String score = matcher.group(3);

                        String username = "";
                        Pattern ignPattern = Pattern.compile("/stats/([^\"]+)");
                        Matcher ignMatcher = ignPattern.matcher(rawPlayerCell);
                        if (ignMatcher.find()) {
                            username = ignMatcher.group(1);
                        }

                        String colorCode = "§f";
                        Pattern colorPattern = Pattern.compile("color:\\s*(#[0-9a-fA-F]{6})");
                        Matcher colorMatcher = colorPattern.matcher(rawPlayerCell);
                        if (colorMatcher.find()) {
                            colorCode = hexToMinecraftColor(colorMatcher.group(1).toUpperCase());
                        }

                        String guildString = "";
                        Pattern guildPattern = Pattern.compile("color:\\s*(#[0-9a-fA-F]{6})[^>]*>\\s*(\\[[^\\]]+\\])\\s*</span>\\s*</a>");
                        Matcher guildMatcher = guildPattern.matcher(rawPlayerCell);
                        if (guildMatcher.find()) {
                            guildString = " " + hexToMinecraftColor(guildMatcher.group(1).toUpperCase()) + guildMatcher.group(2);
                        }

                        client.thePlayer.addChatMessage(new ChatComponentText("§e" + rank + ". " + colorCode + username + guildString + " §7- §e" + score));
                    }

                    if (!foundAny) {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cCould not parse leaderboard data."));
                    }
                    client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                } catch (Exception e) {
                    client.thePlayer.addChatMessage(new ChatComponentText("§cError parsing leaderboard data."));
                }
            });
        }, error -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer != null) client.thePlayer.addChatMessage(new ChatComponentText("§cError fetching leaderboard."));
            });
        });
    }

    private static String hexToMinecraftColor(String hex) {
        switch (hex) {
            case "#0000AA": return "§1";
            case "#00AA00": case "#008000": return "§2";
            case "#00AAAA": return "§3";
            case "#AA0000": return "§4";
            case "#AA00AA": return "§5";
            case "#FFAA00": return "§6";
            case "#AAAAAA": return "§7";
            case "#555555": return "§8";
            case "#5555FF": return "§9";
            case "#55FF55": case "#3CE63C": return "§a";
            case "#55FFFF": case "#3CE6E6": return "§b";
            case "#FF5555": return "§c";
            case "#FF55FF": return "§d";
            case "#FFFF55": return "§e";
            case "#FFFFFF": return "§f";
            default: return "§f";
        }
    }

    private static void fetchStats(Minecraft client, String name) {
        getAsync("https://questviewer-proxy.skorlex.workers.dev/api/stats/" + name, response -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer == null) return;
                try {
                    JsonObject data = new JsonParser().parse(response).getAsJsonObject();
                    if (data.has("success") && data.get("success").getAsBoolean()) {
                        JsonObject payload = data.getAsJsonObject("data");

                        String rankFormatted = payload.get("rankFormatted").getAsString();
                        double level = payload.get("level").getAsDouble();
                        int ap = payload.get("achievementPoints").getAsInt();
                        int quests = payload.get("quests").getAsInt();
                        int challenges = payload.get("challenges").getAsInt();
                        int karma = payload.get("karma").getAsInt();

                        String grammarSuffix = rankFormatted.toLowerCase().endsWith("s") ? "'" : "'s";

                        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                        client.thePlayer.addChatMessage(new ChatComponentText(""));
                        client.thePlayer.addChatMessage(new ChatComponentText(rankFormatted + grammarSuffix + " §f§lGeneral Stats"));
                        client.thePlayer.addChatMessage(new ChatComponentText(""));
                        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7Network Level: §3" + String.format("%.2f", level)));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7Achievement Points: §e" + String.format("%,d", ap)));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7Quests Completed: §b" + String.format("%,d", quests)));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7Challenges Completed: §b" + String.format("%,d", challenges)));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7Karma: §d" + String.format("%,d", karma)));
                        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));

                    } else if (data.has("cause")) {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cError: " + data.get("cause").getAsString()));
                    } else {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not display stats"));
                    }
                } catch (Exception e) {
                    client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not fetch stats from proxy."));
                }
            });
        }, error -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer != null) client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not fetch stats from proxy."));
            });
        });
    }

    private static void fetchSummary(Minecraft client, String ign) {
        getAsync("https://questviewer-proxy.skorlex.workers.dev/api/quests/summary/" + ign, response -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer == null) return;
                try {
                    JsonObject data = new JsonParser().parse(response).getAsJsonObject();
                    if (data.has("success") && data.get("success").getAsBoolean()) {
                        JsonObject payload = data.getAsJsonObject("data");
                        String player = payload.get("player").getAsString();

                        String rankFormatted = payload.has("rankFormatted") ? payload.get("rankFormatted").getAsString() : "§7" + player;

                        int dailiesToday = payload.get("dailiesToday").getAsInt();
                        int totalDailies = payload.get("totalDailies").getAsInt();
                        int weekliesThisWeek = payload.get("weekliesThisWeek").getAsInt();
                        int totalWeeklies = payload.get("totalWeeklies").getAsInt();
                        int completedThisWeek = payload.get("completedThisWeek").getAsInt();
                        int completedThisMonth = payload.get("completedThisMonth").getAsInt();
                        int completedThisYear = payload.get("completedThisYear").getAsInt();

                        String summarySuffix = rankFormatted.toLowerCase().endsWith("s") ? "'" : "'s";

                        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                        client.thePlayer.addChatMessage(new ChatComponentText(""));
                        client.thePlayer.addChatMessage(new ChatComponentText(rankFormatted + summarySuffix + " §f§lQuest Summary"));
                        client.thePlayer.addChatMessage(new ChatComponentText(""));
                        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                        client.thePlayer.addChatMessage(new ChatComponentText("§6§lCurrent Cycle:"));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7Dailies Today: §a" + dailiesToday + " / " + totalDailies + " §7Completed"));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7Weeklies This Week: §a" + weekliesThisWeek + " / " + totalWeeklies + " §7Completed"));
                        client.thePlayer.addChatMessage(new ChatComponentText(""));
                        client.thePlayer.addChatMessage(new ChatComponentText("§6§lTotal Completed:"));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7This Week: §b" + String.format("%,d", completedThisWeek) + " §7Quests"));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7This Month: §b" + String.format("%,d", completedThisMonth) + " §7Quests"));
                        client.thePlayer.addChatMessage(new ChatComponentText("§7This Year: §b" + String.format("%,d", completedThisYear) + " §7Quests"));
                        client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));

                    } else if (data.has("cause")) {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cError: " + data.get("cause").getAsString()));
                    } else {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not display summary"));
                    }
                } catch (Exception e) {
                    client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not display summary"));
                }
            });
        }, error -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer != null) client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not display summary"));
            });
        });
    }

    public static void fetchData(Minecraft client, String ign, String game, boolean weekly) {
        String url = "https://questviewer-proxy.skorlex.workers.dev/api/quests/player_simple/" + ign + "?type=" + (weekly ? "weekly" : "daily") + "&game=" + game;
        getAsync(url, response -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer == null) return;
                try {
                    JsonObject data = new JsonParser().parse(response).getAsJsonObject();
                    if (data.has("success") && data.get("success").getAsBoolean()) {
                        JsonObject questsRoot = data.get("data").getAsJsonObject().get("quests").getAsJsonObject();
                        JsonObject typeObject = questsRoot.get(weekly ? "weekly" : "daily").getAsJsonObject();

                        if (typeObject.entrySet().isEmpty()) {
                            client.thePlayer.addChatMessage(new ChatComponentText("§c[QuestViewer] No quests found for game: " + game));
                            return;
                        }

                        Map.Entry<String, JsonElement> gameKeyValue = typeObject.entrySet().iterator().next();
                        JsonObject questObject = gameKeyValue.getValue().getAsJsonObject();
                        String gameName = questObject.get("name").getAsString();
                        JsonArray questList = questObject.get("quests").getAsJsonArray();

                        if (gameName.equalsIgnoreCase("Classic Games") || gameName.equalsIgnoreCase("Legacy")) {
                            client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                            client.thePlayer.addChatMessage(new ChatComponentText("§eWhich game's " + (weekly ? "weekly" : "daily") + " quests would you like to view?"));
                            client.thePlayer.addChatMessage(new ChatComponentText(""));

                            String cmdType = weekly ? "w" : "d";

                            String targetPlayer = (game.equalsIgnoreCase("current") || game.equalsIgnoreCase("legacy") || game.equalsIgnoreCase("classic")) ? ign : game;

                            sendClickableGame(client, "Arena Brawl", "/q " + cmdType + " arena " + targetPlayer);
                            sendClickableGame(client, "VampireZ", "/q " + cmdType + " vz " + targetPlayer);
                            sendClickableGame(client, "Turbo Kart Racers", "/q " + cmdType + " tkr " + targetPlayer);
                            sendClickableGame(client, "Quakecraft", "/q " + cmdType + " quake " + targetPlayer);
                            sendClickableGame(client, "The Walls", "/q " + cmdType + " walls " + targetPlayer);
                            sendClickableGame(client, "Paintball", "/q " + cmdType + " paintball " + targetPlayer);

                            client.thePlayer.addChatMessage(new ChatComponentText("§m----------------------------------------"));
                            return;
                        }

                        if (questList.size() == 0) {
                            client.thePlayer.addChatMessage(new ChatComponentText("§c" + gameName + " doesn't have any quests!"));
                            return;
                        }

                        client.thePlayer.addChatMessage(new ChatComponentText("§m-------------------"));
                        client.thePlayer.addChatMessage(new ChatComponentText("\n§l" + gameName + "\n§f" + (weekly ? "Weekly" : "Daily") + " Quests\n"));

                        for (JsonElement quest : questList) {
                            JsonObject questObj = quest.getAsJsonObject();
                            JsonObject statusObject = questObj.get("status").getAsJsonObject();
                            client.thePlayer.addChatMessage(new ChatComponentText("§m-------------------"));

                            JsonArray objectives = statusObject.get("objectives").getAsJsonArray();
                            for (JsonElement objElem : objectives) {
                                JsonObject obj = objElem.getAsJsonObject();
                                String desc = obj.get("description").getAsString();
                                int progress = obj.get("progress").getAsInt();
                                int goal = obj.get("goal").getAsInt();

                                String color = (progress >= goal) ? "§a" : (progress == 0 ? "§c" : "§e");

                                String[] lines = desc.split("\n");
                                for (String line : lines) {
                                    client.thePlayer.addChatMessage(new ChatComponentText("§f" + line.trim()));
                                }

                                client.thePlayer.addChatMessage(new ChatComponentText(color + progress + "/" + goal));
                            }
                        }
                        client.thePlayer.addChatMessage(new ChatComponentText("§m-------------------"));
                    } else if (data.has("cause")) {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cError: " + data.get("cause").getAsString()));
                    } else {
                        client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not display quests"));
                    }
                } catch (Exception e) {
                    client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not display quests"));
                }
            });
        }, error -> {
            client.addScheduledTask(() -> {
                if (client.thePlayer != null) client.thePlayer.addChatMessage(new ChatComponentText("§cError: Could not display quests"));
            });
        });
    }

    private static void sendClickableGame(Minecraft client, String name, String command) {
        ChatComponentText comp = new ChatComponentText("§7- §b§l" + name);
        comp.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        comp.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("§eClick to view " + name + " quests")));
        client.thePlayer.addChatMessage(comp);
    }
}