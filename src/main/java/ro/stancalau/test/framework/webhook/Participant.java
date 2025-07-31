package ro.stancalau.test.framework.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Participant {
    
    @JsonProperty("sid")
    private String sid;
    
    @JsonProperty("identity")
    private String identity;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("joinedAt")
    private String joinedAt;
    
    @JsonProperty("metadata")
    private String metadata;
    
    @JsonProperty("attributes")
    private java.util.Map<String, String> attributes;
}