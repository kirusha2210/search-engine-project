package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;

import java.util.Optional;

@Repository
public interface IndexRepository  extends JpaRepository<IndexModel, Integer> {
    Optional<IndexModel> findByLemmaId(LemmaModel lemmaId);
    float findRankByLemmaIdAndPageId(LemmaModel lemmaId, PageModel pageId);
}
