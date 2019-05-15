package codes.biscuit.lobbyglow.utils;

@SuppressWarnings("unused")
public class APIPojo {

    private boolean success;
    private PlayerPojo player;

    public boolean isSuccess() {
        return success;
    }

    public PlayerPojo getPlayer() {
        return player;
    }

    public class PlayerPojo {

        private Boolean battlePassGlowStatus;

        public Boolean isBattlePassGlowStatus() {
            return battlePassGlowStatus;
        }
    }
}
