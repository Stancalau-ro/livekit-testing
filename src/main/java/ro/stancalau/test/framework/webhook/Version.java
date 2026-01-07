package ro.stancalau.test.framework.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Version {

    @JsonProperty("unixMicro")
    private String unixMicro;
}
