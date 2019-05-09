package codes.biscuit.lobbyglow;

import codes.biscuit.lobbyglow.utils.Utils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = LobbyGlow.MOD_ID, version = LobbyGlow.VERSION, name = LobbyGlow.MOD_NAME, acceptedMinecraftVersions = "[1.8,1.8.9]")
public class LobbyGlow {

    public static final String MOD_ID = "lobbyglow";
    public static final String MOD_NAME = "LobbyGlow";
    public static final String VERSION = "1.0";
    public static LobbyGlow INSTANCE;

    private Utils utils = new Utils();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        INSTANCE = this;
        utils.runLobbyCheckerTimer();
    }

    public Utils getUtils() {
        return utils;
    }
}
