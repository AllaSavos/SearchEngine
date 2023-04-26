package searchengine.parse;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@Data
@ConfigurationProperties(prefix = "indexing-settings")
public class Config {
    private List<String> url = new ArrayList<>();
    private List<String> name = new ArrayList<>();
    private String userAgent;
    private String referrer;
}
