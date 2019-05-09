package codes.biscuit.lobbyglow.utils;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Utils {

    private Map<UUID, Boolean> glowingCache = new HashMap<>(); //TODO add a cache cleanup so it doesn't infinitely fill up
    private int lastMinuteQueries = 0; // to avoid hitting limit of 120
    private int currentThreads = 0;
    private static String COMPASS_NAME = EnumChatFormatting.GREEN+"Game Menu "+EnumChatFormatting.GRAY+"(Right Click)";
    private static String COMPASS_LORE = EnumChatFormatting.DARK_PURPLE.toString()+EnumChatFormatting.ITALIC.toString()+EnumChatFormatting.GRAY+"Right Click to bring up the Game Menu!";
    private boolean inHypixelLobby = false;

    public boolean shouldGlow(Entity entity) {
        if (!entity.getName().startsWith("\u007a") && !entity.getName().contains(" ")) { // Don't check NPCs etc.
            UUID uuid = entity.getUniqueID();
            if (glowingCache.containsKey(uuid)) {
                if (glowingCache.get(uuid) != null) {
                    return glowingCache.get(uuid);
                }
            } else {
                if (currentThreads < 10 && lastMinuteQueries < 90) {
                    glowingCache.put(uuid, null);
                    grabAPI(uuid);
                }
            }
        }
        return false;
    }

    private void grabAPI(UUID uuid) {
        new Thread(() -> {
            HttpsURLConnection conn = null;
            try {
                URL url = new URL(("https://api.hypixel.net/player" +
                        "?key=" + URLEncoder.encode("your-key-here", "UTF-8") // TODO My key for now, will replace later
                        + "&uuid=" + URLEncoder.encode(uuid.toString(), "UTF-8")));
                lastMinuteQueries++;
                currentThreads++;
                conn = (HttpsURLConnection)url.openConnection();
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
                        glowingCache.put(uuid, isGlowing);
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

    public void runLobbyCheckerTimer() { // Maybe not the best method to check if in lobby, but works
        Minecraft mc = Minecraft.getMinecraft();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mc != null && mc.thePlayer != null && mc.thePlayer.inventory != null) {
                    ItemStack item = mc.thePlayer.inventory.getStackInSlot(0);
                    if (item != null && item.hasDisplayName() && item.getDisplayName().equals(COMPASS_NAME)) {
                        List<String> toolip = item.getTooltip(mc.thePlayer, false);
                        inHypixelLobby = toolip.get(1).equals(COMPASS_LORE);
                    }
                }
            }
        }, 1000, 1000);
    }

    public boolean isInHypixelLobby() {
        return inHypixelLobby;
    }
}
