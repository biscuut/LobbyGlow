package codes.biscuit.lobbyglow.utils;

import codes.biscuit.lobbyglow.LobbyGlow;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Utils {

    private Map<UUID, Boolean> glowingCache = new HashMap<>();
    private int lastMinuteQueries = 0; // to avoid hitting limit of 120
    private int currentThreads = 0;
    private static String COMPASS_NAME = EnumChatFormatting.GREEN+"Game Menu "+EnumChatFormatting.GRAY+"(Right Click)";
    private static String COMPASS_LORE = EnumChatFormatting.DARK_PURPLE.toString()+EnumChatFormatting.ITALIC.toString()+EnumChatFormatting.GRAY+"Right Click to bring up the Game Menu!";
    private boolean inHypixelLobby = false;
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
        }, 1000, 1000);
    }

    public boolean isInHypixelLobby() {
        return inHypixelLobby;
    }

    public boolean isOnHypixel() {
        if (Minecraft.getMinecraft().getCurrentServerData() != null) {
            String ip = Minecraft.getMinecraft().getCurrentServerData().serverIP.toLowerCase();
            return (ip.equals("hypixel.net") || ip.endsWith(".hypixel.net") || ip.contains(".hypixel.net:") || ip.startsWith("hypixel.net:"));
        } else {
            return false;
        }
    }

    public static void sendMessage(IChatComponent sendMessage) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(sendMessage);
        MinecraftForge.EVENT_BUS.post(new ClientChatReceivedEvent((byte)1, sendMessage));
    }
}
