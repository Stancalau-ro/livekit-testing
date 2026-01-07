package ro.stancalau.test.framework.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import livekit.LivekitIngress.IngressInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class IngressStateManager {

    private final Map<String, IngressInfo> ingresses = new ConcurrentHashMap<>();
    private final Map<String, String> activeStreams = new ConcurrentHashMap<>();
    private List<IngressInfo> lastListResult;

    public IngressStateManager() {}

    public void registerIngress(String name, IngressInfo info) {
        ingresses.put(name, info);
        log.debug("Registered ingress: {} with id: {}", name, info.getIngressId());
    }

    public IngressInfo getIngress(String name) {
        return ingresses.get(name);
    }

    public void removeIngress(String name) {
        ingresses.remove(name);
        log.debug("Removed ingress: {}", name);
    }

    public Map<String, IngressInfo> getAllIngresses() {
        return new HashMap<>(ingresses);
    }

    public void registerActiveStream(String ingressName, String containerAlias) {
        activeStreams.put(ingressName, containerAlias);
        log.debug("Registered active stream for ingress {} from container {}", ingressName, containerAlias);
    }

    public String getActiveStreamContainer(String ingressName) {
        return activeStreams.get(ingressName);
    }

    public void removeActiveStream(String ingressName) {
        activeStreams.remove(ingressName);
        log.debug("Removed active stream for ingress {}", ingressName);
    }

    public void setLastListResult(List<IngressInfo> result) {
        this.lastListResult = result;
        log.debug("Stored ingress list result with {} items", result.size());
    }

    public void clearAll() {
        ingresses.clear();
        activeStreams.clear();
        lastListResult = null;
        log.debug("Cleared all ingress state");
    }
}
