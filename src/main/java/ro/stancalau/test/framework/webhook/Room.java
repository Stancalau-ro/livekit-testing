package ro.stancalau.test.framework.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Room {
    
    @JsonProperty("sid")
    private String sid;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("emptyTimeout")
    private Integer emptyTimeout;
    
    @JsonProperty("departureTimeout")
    private Integer departureTimeout;
    
    @JsonProperty("creationTime")
    private String creationTime;
    
    @JsonProperty("creationTimeMs")
    private String creationTimeMs;
    
    @JsonProperty("turnPassword")
    private String turnPassword;
    
    @JsonProperty("enabledCodecs")
    private List<Codec> enabledCodecs;
}