package ro.stancalau.test.framework.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackCodec {
    
    @JsonProperty("mimeType")
    private String mimeType;
    
    @JsonProperty("mid")
    private String mid;
    
    @JsonProperty("cid")
    private String cid;
    
    @JsonProperty("layers")
    private List<Layer> layers;
}