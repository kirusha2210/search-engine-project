package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import searchengine.model.SiteModel;

@Data
@AllArgsConstructor
public class SearchData {
    String site;
    String siteName;
    String uri;
    String[] snippet;
    float relevance;
}
