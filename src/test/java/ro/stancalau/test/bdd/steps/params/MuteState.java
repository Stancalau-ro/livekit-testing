package ro.stancalau.test.bdd.steps.params;

public enum MuteState {
  muted(true),
  unmuted(false);

  private final boolean isMuted;

  MuteState(boolean isMuted) {
    this.isMuted = isMuted;
  }

  public boolean isMuted() {
    return isMuted;
  }
}
