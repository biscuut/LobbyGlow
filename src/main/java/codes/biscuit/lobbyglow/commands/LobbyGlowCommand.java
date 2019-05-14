package codes.biscuit.lobbyglow.commands;

import codes.biscuit.lobbyglow.LobbyGlow;
import codes.biscuit.lobbyglow.utils.Utils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            }
        }
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "--------------" + EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + " LobbyGlow " + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + EnumChatFormatting.STRIKETHROUGH + "--------------"));
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GOLD + "\u25CF /lg key <key|clear> " + EnumChatFormatting.GRAY + "- Enter your Hypixel API key manually."));
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.ITALIC + "v1.0" + " by Biscut"));
        Utils.sendMessage(new ChatComponentText(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.STRIKETHROUGH + "---------------------------------------"));
    }
}
