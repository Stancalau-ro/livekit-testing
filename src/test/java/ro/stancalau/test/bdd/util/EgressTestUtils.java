package ro.stancalau.test.bdd.util;

import io.livekit.server.EgressServiceClient;
import java.util.concurrent.atomic.AtomicReference;
import livekit.LivekitEgress;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EgressTestUtils {

    private static final long POLL_TIMEOUT_MS = 60_000;
    private static final long POLL_INTERVAL_MS = 1_000;

    public static void waitForEgressToBeActive(EgressServiceClient egressClient, String egressId, String roomName)
            throws Exception {
        AtomicReference<String> errorRef = new AtomicReference<>();

        boolean success = BrowserPollingHelper.pollForCondition(
                () -> {
                    try {
                        LivekitEgress.EgressInfo info = egressClient.listEgress(roomName).execute().body().stream()
                                .filter(e -> e.getEgressId().equals(egressId))
                                .findFirst()
                                .orElse(null);

                        if (info != null) {
                            LivekitEgress.EgressStatus status = info.getStatus();
                            log.debug("Egress {} status: {}", egressId, status);

                            if (status == LivekitEgress.EgressStatus.EGRESS_ACTIVE) {
                                log.info("Egress {} is now active", egressId);
                                return true;
                            } else if (status == LivekitEgress.EgressStatus.EGRESS_FAILED
                                    || status == LivekitEgress.EgressStatus.EGRESS_ABORTED
                                    || status == LivekitEgress.EgressStatus.EGRESS_COMPLETE) {
                                errorRef.set("Egress "
                                        + egressId
                                        + " failed to start. Status: "
                                        + status
                                        + ". Error: "
                                        + info.getError());
                                return true;
                            }
                        }
                        return false;
                    } catch (Exception e) {
                        log.warn("Error checking egress status: {}", e.getMessage());
                        return false;
                    }
                },
                POLL_TIMEOUT_MS,
                POLL_INTERVAL_MS);

        String error = errorRef.get();
        if (error != null) {
            throw new IllegalStateException(error);
        }
        if (!success) {
            throw new IllegalStateException(
                    "Egress " + egressId + " did not become active after " + (POLL_TIMEOUT_MS / 1000) + " seconds");
        }
    }
}
