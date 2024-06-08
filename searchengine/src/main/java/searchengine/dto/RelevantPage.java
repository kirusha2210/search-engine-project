package searchengine.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.PageModel;

@Getter
@Setter
public class RelevantPage {
    private final PageModel pageModel;
    private float absolutRelevance;
    private float relativeRelevance;

    @Autowired
    public RelevantPage(PageModel pageModel, float relativeRelevance, float absolutRelevance) {
        this.pageModel = pageModel;
        this.absolutRelevance = absolutRelevance;
        this.relativeRelevance = relativeRelevance;
    }

//    public void add(PageModel pageModel, float relativeRelevance, float absolutRelevance) {
//        new RelevantPage(pageModel, relativeRelevance, absolutRelevance);
//    }
}
