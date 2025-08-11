package ro.stancalau.test.framework.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookEvent {
    
    @JsonProperty("event")
    private String event;
    
    @JsonProperty("room")
    private Room room;
    
    @JsonProperty("participant")
    private Participant participant;
    
    @JsonProperty("track")
    private Track track;
    
    @JsonProperty("egressInfo")
    private EgressInfo egressInfo;
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("createdAt")
    private String createdAt;
}