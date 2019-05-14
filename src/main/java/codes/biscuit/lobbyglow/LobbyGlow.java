package codes.biscuit.lobbyglow;

import codes.biscuit.lobbyglow.commands.LobbyGlowCommand;
import codes.biscuit.lobbyglow.listeners.PlayerListener;
import codes.biscuit.lobbyglow.utils.ConfigValues;
import codes.biscuit.lobbyglow.utils.Utils;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@SuppressWarnings("WeakerAccess")
@Mod(modid = LobbyGlow.MOD_ID, version = LobbyGlow.VERSION, name = LobbyGlow.MOD_NAME, acceptedMinecraftVersions = "[1.8,1.8.9]")
public class LobbyGlow {

    public static final String MOD_ID = "lobbyglow";
    public static final String MOD_NAME = "LobbyGlow";
    public static final String VERSION = "1.0";
    public static LobbyGlow INSTANCE;

    private Utils utils = new Utils(this);
    private ConfigValues configValues;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        INSTANCE = this;
        configValues = new ConfigValues(e.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLPostInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(new PlayerListener(this));
        ClientCommandHandler.instance.registerCommand(new LobbyGlowCommand(this));
        utils.runLobbyCheckerTimer();
        configValues.loadConfig();
    }

    public Utils getUtils() {
        return utils;
    }

    public ConfigValues getConfigValues() {
        return configValues;
    }
}
