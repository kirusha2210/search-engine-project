package searchengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import searchengine.dto.search.SearchData;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Massage {
    private boolean result;
    private String error;
    private List<SearchData> data;
    int count;

    public Massage(boolean result) {
        this.result = result;
    }

    public Massage(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
    public Massage(boolean result, List<SearchData> data, int count) {
        this.result = result;
        this.data = data;
        this.count = count;
    }
}

