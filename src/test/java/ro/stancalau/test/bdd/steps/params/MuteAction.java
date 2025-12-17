package ro.stancalau.test.bdd.steps.params;

public enum MuteAction {
    mutes(true),
    unmutes(false);

    private final boolean shouldMute;

    MuteAction(boolean shouldMute) {
        this.shouldMute = shouldMute;
    }

    public boolean shouldMute() {
        return shouldMute;
    }
}
