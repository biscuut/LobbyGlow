package codes.biscuit.lobbyglow.listeners;

import codes.biscuit.lobbyglow.LobbyGlow;
import codes.biscuit.lobbyglow.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.regex.Pattern;

public class PlayerListener {

    private LobbyGlow main;
    private int tickCounter = 0;

    public PlayerListener(LobbyGlow main) {
        this.main = main;
    }

    @SubscribeEvent
    public void onClientChatReceive(ClientChatReceivedEvent e) {
        String message = e.message.getUnformattedText();
        if (message.startsWith("Your new API key is ")) {
            String key = message.split(Pattern.quote("Your new API key is "))[1];
            main.getConfigValues().setKey(key);
            Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GREEN+"Successfully updated your Hypixel API in LobbyGlow!"));
            main.getConfigValues().saveConfig();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (tickCounter >= 0) {
            Minecraft mc = Minecraft.getMinecraft();
            if (e.phase == TickEvent.Phase.START && mc != null && mc.thePlayer != null && mc.theWorld != null && main.getConfigValues().getKey().equals("") && main.getUtils().isOnHypixel()) {
                tickCounter++;
                if (tickCounter > 50) {
                    Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "--------------" + EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + " LobbyGlow " + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + EnumChatFormatting.STRIKETHROUGH + "--------------"));
                    IChatComponent message = new ChatComponentText(EnumChatFormatting.RED + "It seems you haven't set your Hypixel API key for LobbyGlow. Click here to create a new one automatically or do /lg key <key> to set it manually.");
                    message.setChatStyle(message.getChatStyle().setChatHoverEvent( // Add the translation as hover text and send the message
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.RED + "Click here to generate a new key!"))));
                    message.setChatStyle(message.getChatStyle().setChatClickEvent( // Add the translation as hover text and send the message
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/api new")));
                    Utils.sendMessage(message);
                    Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "---------------------------------------"));
                    tickCounter = -1;
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientDisconnectionFromServer(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        tickCounter = 0;
    }
}
