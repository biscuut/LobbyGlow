package codes.biscuit.lobbyglow.utils;

@SuppressWarnings("unused")
class APIPojo {

    private boolean success;
    private PlayerPojo player;

    boolean isSuccess() {
        return success;
    }

    PlayerPojo getPlayer() {
        return player;
    }

    public class PlayerPojo {
        private boolean battlePassGlowStatus;

        boolean isBattlePassGlowStatus() {
            return battlePassGlowStatus;
        }
    }
}
