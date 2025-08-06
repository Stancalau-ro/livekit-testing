package ro.stancalau.test.bdd.util;

import io.livekit.server.EgressServiceClient;
import livekit.LivekitEgress;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EgressTestUtils {

    public static void waitForEgressToBeActive(EgressServiceClient egressClient, String egressId, String roomName) throws Exception {
        int maxAttempts = 30;
        for (int i = 0; i < maxAttempts; i++) {
            LivekitEgress.EgressInfo info = egressClient.listEgress(roomName).execute().body().stream()
                    .filter(e -> e.getEgressId().equals(egressId))
                    .findFirst()
                    .orElse(null);
            
            if (info != null) {
                LivekitEgress.EgressStatus status = info.getStatus();
                log.debug("Egress {} status: {}", egressId, status);
                
                if (status == LivekitEgress.EgressStatus.EGRESS_ACTIVE) {
                    log.info("Egress {} is now active", egressId);
                    return;
                } else if (status == LivekitEgress.EgressStatus.EGRESS_FAILED || 
                          status == LivekitEgress.EgressStatus.EGRESS_ABORTED ||
                          status == LivekitEgress.EgressStatus.EGRESS_COMPLETE) {
                    throw new IllegalStateException("Egress " + egressId + " failed to start. Status: " + status + 
                            ". Error: " + info.getError());
                }
            }
            
            Thread.sleep(1000);
        }
        throw new IllegalStateException("Egress " + egressId + " did not become active after " + maxAttempts + " seconds");
    }
}