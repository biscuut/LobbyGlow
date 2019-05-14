package codes.biscuit.lobbyglow.utils;

import codes.biscuit.lobbyglow.LobbyGlow;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class Utils {

    public Map<UUID, Boolean> glowingCache = new HashMap<>();
    private int lastMinuteQueries = 0; // to avoid hitting limit of 120
    private int currentThreads = 0;
    private static String COMPASS_NAME = EnumChatFormatting.GREEN+"Game Menu "+EnumChatFormatting.GRAY+"(Right Click)";
    private static String COMPASS_LORE = EnumChatFormatting.DARK_PURPLE.toString()+EnumChatFormatting.ITALIC.toString()+EnumChatFormatting.GRAY+"Right Click to bring up the Game Menu!";
    private boolean inHypixelLobby = false;
    private boolean onHypixel = false;
    private LobbyGlow main;

    public Utils(LobbyGlow main) {
        this.main = main;
    }

    public boolean shouldGlow(Entity entity) {
        if (entity.getName().matches("[a-zA-Z0-9]*")) { // Don't check NPCs etc.
            UUID uuid = entity.getUniqueID();
            if (glowingCache.containsKey(uuid)) {
                if (glowingCache.get(uuid) != null) {
                    return glowingCache.get(uuid);
                }
            } else {
                int QUERIES_PER_MINUTE = 80;
                int CONCURRENT_THREADS = 10;

                if (currentThreads < CONCURRENT_THREADS && lastMinuteQueries < QUERIES_PER_MINUTE) {
                    addCacheEntry(uuid, null);
                    grabAPI(uuid);
                }
            }
        }
        return false;
    }

    private void addCacheEntry(UUID uuid, Boolean status) {
        if (glowingCache.size() > 2000) { // Avoid caching too many
            glowingCache.clear();
        }
        glowingCache.put(uuid, status);
    }


    private void grabAPI(UUID uuid) {
        if (!main.getConfigValues().getKey().equals("")) {
            new Thread(() -> {
                HttpsURLConnection conn = null;
                try {
                    URL url = new URL(("https://api.hypixel.net/player" +
                            "?key=" + URLEncoder.encode(main.getConfigValues().getKey(), "UTF-8")
                            + "&uuid=" + URLEncoder.encode(uuid.toString(), "UTF-8")));
                    lastMinuteQueries++;
                    currentThreads++;
                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestProperty("Content-Type", "text/plain; charset=" + "UTF-8");
                    conn.setRequestProperty("Accept-Charset", "UTF-8");
                    conn.setRequestMethod("GET");
                    int responseCode = conn.getResponseCode();
                    StringBuilder outputBuilder = new StringBuilder();
                    String nextLine;
                    if (conn.getInputStream() != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        while (null != (nextLine = reader.readLine())) {
                            outputBuilder.append(nextLine);
                        }
                    }
                    String result = outputBuilder.toString();
                    if (responseCode != 200) {
                        throw new Exception();
                    }
                    APIPojo apiPojo = new Gson().fromJson(result, APIPojo.class);
                    if (apiPojo.isSuccess()) {
                        if (apiPojo.getPlayer() != null) {
                            boolean isGlowing = apiPojo.getPlayer().isBattlePassGlowStatus();
                            addCacheEntry(uuid, isGlowing);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                    currentThreads--;
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            lastMinuteQueries--;
                        }
                    }, 60000);
                }
            }).start();
        }
    }

    public void runLobbyCheckerTimer() { // Maybe not the best method to check if in lobby, but works
        Minecraft mc = Minecraft.getMinecraft();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForItem(mc);
                checkIP(mc);
            }
        }, 1000, 1000);
    }

    private void checkForItem(Minecraft mc) {
        if (mc != null && mc.thePlayer != null && mc.thePlayer.inventory != null) {
            ItemStack item = mc.thePlayer.inventory.getStackInSlot(0);
            if (item != null && item.hasDisplayName() && item.getDisplayName().equals(COMPASS_NAME)) {
                List<String> toolip = item.getTooltip(mc.thePlayer, false);
                if (toolip.get(1).equals(COMPASS_LORE)) {
                    inHypixelLobby = true;
                    return;
                }
            }
        }
        inHypixelLobby = false;
    }

    private void checkIP(Minecraft mc) {
        if (mc.getCurrentServerData() != null) {
            String ip = mc.getCurrentServerData().serverIP.toLowerCase();
            if (ip.equals("hypixel.net") || ip.endsWith(".hypixel.net") || ip.contains(".hypixel.net:") || ip.startsWith("hypixel.net:")) {
                onHypixel = true;
                return;
            }
        }
        onHypixel = false;
    }

    public boolean isInHypixelLobby() {
        return inHypixelLobby;
    }

    public boolean isOnHypixel() {
        return onHypixel;
    }

    public static void sendMessage(IChatComponent sendMessage) {
        ClientChatReceivedEvent event = new ClientChatReceivedEvent((byte)1, sendMessage);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(event.message);
        }
    }

    public void checkUpdates() {
        new Thread(() -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/biscuut/LobbyGlow/master/build.gradle");
                URLConnection connection = url.openConnection();
                connection.setReadTimeout(5000);
                connection.addRequestProperty("User-Agent", "LobbyGlow update checker");
                connection.setDoOutput(true);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String currentLine;
                String newestVersion = "";
                while ((currentLine = reader.readLine()) != null) {
                    if (currentLine.contains("version = \"")) {
                        String[] newestVersionSplit = currentLine.split(Pattern.quote("version = \""));
                        newestVersionSplit = newestVersionSplit[1].split(Pattern.quote("\""));
                        newestVersion = newestVersionSplit[0];
                        break;
                    }
                }
                reader.close();
                List<Integer> newestVersionNumbers = new ArrayList<>();
                List<Integer> thisVersionNumbers = new ArrayList<>();
                try {
                    for (String s : newestVersion.split(Pattern.quote("."))) {
                        newestVersionNumbers.add(Integer.parseInt(s));
                    }
                    for (String s : LobbyGlow.VERSION.split(Pattern.quote("."))) {
                        thisVersionNumbers.add(Integer.parseInt(s));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }
                for (int i = 0; i < 3; i++) {
                    if (i >= newestVersionNumbers.size() ) {
                        newestVersionNumbers.add(i, 0);
                    }
                    if (i >= thisVersionNumbers.size()) {
                        thisVersionNumbers.add(i, 0);
                    }
                    if (newestVersionNumbers.get(i) > thisVersionNumbers.get(i)) {
                        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "--------------" + EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + " LobbyGlow " + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + EnumChatFormatting.STRIKETHROUGH + "--------------"));
                        ChatComponentText newVersion = new ChatComponentText(EnumChatFormatting.GREEN+"A new version, " + newestVersion + " is available. Download it by clicking here.");
                        newVersion.setChatStyle(newVersion.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/biscuut/LobbyGlow/releases")));
                        Utils.sendMessage(newVersion);
                        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "---------------------------------------"));
                        break;
                    } else if (thisVersionNumbers.get(i) > newestVersionNumbers.get(i)) {
                        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "--------------" + EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + " LobbyGlow " + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + EnumChatFormatting.STRIKETHROUGH + "--------------"));
                        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GOLD + "You are running a development version: " + LobbyGlow.VERSION + ". The latest online version is " + newestVersion + "."));
                        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "---------------------------------------"));
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}
