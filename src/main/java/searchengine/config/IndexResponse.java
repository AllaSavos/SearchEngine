package searchengine.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
public class IndexResponse {
    @JsonProperty("result")
    private boolean result;
    private String error;

}
