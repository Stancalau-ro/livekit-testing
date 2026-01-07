package ro.stancalau.test.bdd.steps.params;

import io.cucumber.java.ParameterType;

public class ParameterTypes {

    @ParameterType("muted|unmuted")
    public MuteState muteState(String state) {
        return MuteState.valueOf(state);
    }

    @ParameterType("mutes|unmutes")
    public MuteAction muteAction(String action) {
        return MuteAction.valueOf(action);
    }

    @ParameterType("enabled|disabled")
    public EnabledState enabledState(String state) {
        return EnabledState.valueOf(state);
    }

    @ParameterType("inactive|buffering|publishing|error")
    public IngressState ingressState(String state) {
        return IngressState.valueOf(state);
    }
}
