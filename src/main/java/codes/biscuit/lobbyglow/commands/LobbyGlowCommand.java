package codes.biscuit.lobbyglow.commands;

import codes.biscuit.lobbyglow.LobbyGlow;
import codes.biscuit.lobbyglow.utils.APIPojo;
import codes.biscuit.lobbyglow.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class LobbyGlowCommand extends CommandBase {

    private LobbyGlow main;

    public LobbyGlowCommand(LobbyGlow main) {
        this.main = main;
    }

    @Override
    public String getCommandName() {
        return "lobbyglow";
    }

    @Override
    public List<String> getCommandAliases()
    {
        return Collections.singletonList("lg");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return null;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender)
    {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos)
    {
        if (args.length == 1) {
            List<String> arguments = new ArrayList<>(Collections.singletonList("key"));
            arguments.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return arguments;
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("key")) {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("clear")) {
                        main.getConfigValues().setKey("");
                        main.getConfigValues().saveConfig();
                        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Your key has been cleared!"));
                    } else {
                        main.getConfigValues().setKey(args[1]);
                        main.getConfigValues().saveConfig();
                        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Your key has been updated!"));
                    }
                } else {
                    Utils.sendMessage(new ChatComponentText(EnumChatFormatting.RED + "Please specify a key: /lg key <key>"));
                }
                return;
            } else if (args[0].equalsIgnoreCase("clear")) {
                main.getUtils().clearCache();
                Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GREEN + "All glowing players have been cleared from the cache!"));
                return;
            } else if (args[0].equalsIgnoreCase("toggle")) {
                if (main.getConfigValues().enabled) {
                    Utils.sendMessage(new ChatComponentText(EnumChatFormatting.RED + "LobbyGlow is now disabled!"));
                } else {
                    Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GREEN + "LobbyGlow is now enabled!"));
                }
                main.getConfigValues().enabled = !main.getConfigValues().enabled;
                return;
            } else if (args[0].equalsIgnoreCase("test")) {
                if (args.length > 1) {
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(args[1]);
                    } catch (IllegalArgumentException ex) {
                        for (NetworkPlayerInfo info : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
                            if (info.getGameProfile().getName().equalsIgnoreCase(args[1])) {
                                uuid = info.getGameProfile().getId();
                            }
                        }
                    }
                    if (uuid != null) {
                        UUID finalUuid = uuid;
                        if (main.getUtils().getLastMinuteQueries() < 110) {
                            new Thread(() -> {
                                APIPojo response = main.getUtils().getAPIResponse(finalUuid);
                                if (response != null) {
                                    if (response.isSuccess()) {
                                        if (response.getPlayer().isBattlePassGlowStatus() != null) {
                                            if (response.getPlayer().isBattlePassGlowStatus()) {
                                                Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GREEN + "The connection was successful! This player's glow status is true."));
                                            } else {
                                                Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GREEN + "The connection was successful! This player's glow status is " + EnumChatFormatting.RED + "false" + EnumChatFormatting.GREEN + "."));
                                            }
                                        } else {
                                            Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GREEN + "The connection was successful! This player's glow status is " + EnumChatFormatting.RED + "not set" + EnumChatFormatting.GREEN + ". If this player's glow is supposed to be enabled, they must toggle their glow twice for it to be set in the API."));
                                        }
                                    } else {
                                        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.RED + "The connection was successful but the response was not a success."));
                                    }
                                } else {
                                    Utils.sendMessage(new ChatComponentText(EnumChatFormatting.RED + "An error occurred in the connection and it was not successful!"));
                                }
                            }).start();
                        } else {
                            Utils.sendMessage(new ChatComponentText(EnumChatFormatting.RED + "You've made too many API requests recently! Please try again in a minute."));
                        }
                    } else {
                        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.RED + "This player is not in your game!"));
                    }
                } else {
                    Utils.sendMessage(new ChatComponentText(EnumChatFormatting.RED + "Please specify a player/uuid: /lg test <player|uuid>"));
                }
                return;
            }
        }
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "--------------" + EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + " LobbyGlow " + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + EnumChatFormatting.STRIKETHROUGH + "--------------"));
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GOLD + "\u25CF /lg key <key|clear> " + EnumChatFormatting.GRAY + "- Enter your Hypixel API key manually."));
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GOLD + "\u25CF /lg clear " + EnumChatFormatting.GRAY + "- "+EnumChatFormatting.YELLOW+"[DEBUG]"+EnumChatFormatting.GRAY+" Remove all current glowing players from the cache (will download again after)."));
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GOLD + "\u25CF /lg test <player|uuid> " + EnumChatFormatting.GRAY + "- "+EnumChatFormatting.YELLOW+"[DEBUG]"+EnumChatFormatting.GRAY+" Test your connection and the target player's glow status on the API."));
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.ITALIC + "v"+LobbyGlow.VERSION+"" + " by Biscut"));
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "---------------------------------------"));
    }
}
