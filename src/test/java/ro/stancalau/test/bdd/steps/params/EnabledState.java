package ro.stancalau.test.bdd.steps.params;

public enum EnabledState {
  enabled(true),
  disabled(false);

  private final boolean isEnabled;

  EnabledState(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  public boolean isEnabled() {
    return isEnabled;
  }
}
