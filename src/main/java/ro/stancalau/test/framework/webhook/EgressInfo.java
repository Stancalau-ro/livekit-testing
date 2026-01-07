package ro.stancalau.test.framework.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EgressInfo {

    @JsonProperty("egressId")
    private String egressId;

    @JsonProperty("roomId")
    private String roomId;

    @JsonProperty("roomName")
    private String roomName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("startedAt")
    private Long startedAt;

    @JsonProperty("endedAt")
    private Long endedAt;

    @JsonProperty("error")
    private String error;

    @JsonProperty("fileResults")
    private Object fileResults;

    @JsonProperty("streamResults")
    private Object streamResults;

    @JsonProperty("segmentResults")
    private Object segmentResults;
}
