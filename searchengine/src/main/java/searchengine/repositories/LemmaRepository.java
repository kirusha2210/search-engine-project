package searchengine.repositories;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;

import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    Optional<LemmaModel> findBySiteIdAndLemma(SiteModel siteId, String lemma);
}
