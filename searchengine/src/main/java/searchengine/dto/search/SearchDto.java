package searchengine.dto.search;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.RequestBody;
import searchengine.config.Site;

@Data
public class SearchDto {
    private String query;
    private String site;
    private String offset;
    private String limit;
}
