package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Massage {
    private boolean result;
    private String error;
    private SearchData data;
    int count;

    public Massage(boolean result) {
        this.result = result;
    }

    public Massage(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
    public Massage(boolean result, SearchData data, int count) {
        this.result = result;
        this.data = data;
        this.count = count;
    }
}

