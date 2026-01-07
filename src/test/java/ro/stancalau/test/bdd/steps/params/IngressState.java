package ro.stancalau.test.bdd.steps.params;

public enum IngressState {
    inactive("ENDPOINT_INACTIVE"),
    buffering("ENDPOINT_BUFFERING"),
    publishing("ENDPOINT_PUBLISHING"),
    error("ENDPOINT_ERROR");

    private final String sdkState;

    IngressState(String sdkState) {
        this.sdkState = sdkState;
    }

    public String toSdkState() {
        return sdkState;
    }

    public static IngressState fromSdkState(String sdkState) {
        for (IngressState state : values()) {
            if (state.sdkState.equals(sdkState)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown SDK ingress state: " + sdkState);
    }
}
