package searchengine.dto.search;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.RequestBody;

@Data
public class SearchDto {
    private String query;
    private String siteUrl;
    private int offset = 0;
    private int limit = 20;
}
