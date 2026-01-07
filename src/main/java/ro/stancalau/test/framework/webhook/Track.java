package ro.stancalau.test.framework.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Track {

    @JsonProperty("sid")
    private String sid;

    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("muted")
    private Boolean muted;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("simulcast")
    private Boolean simulcast;

    @JsonProperty("source")
    private String source;

    @JsonProperty("layers")
    private List<Layer> layers;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("mid")
    private String mid;

    @JsonProperty("codecs")
    private List<TrackCodec> codecs;

    @JsonProperty("stream")
    private String stream;

    @JsonProperty("version")
    private Version version;

    public String getEffectiveType() {
        if (type != null) {
            return type;
        }

        if (mimeType != null && mimeType.startsWith("audio/")) {
            return "AUDIO";
        }

        if (source != null) {
            switch (source) {
                case "MICROPHONE":
                    return "AUDIO";
                case "CAMERA":
                case "SCREEN_SHARE":
                case "SCREEN_SHARE_AUDIO":
                    return source.contains("AUDIO") ? "AUDIO" : "VIDEO";
                default:
                    break;
            }
        }

        return null;
    }
}
