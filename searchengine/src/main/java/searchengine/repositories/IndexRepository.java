package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;

import java.util.Optional;

@Repository
public interface IndexRepository  extends JpaRepository<IndexModel, Integer> {
    Optional<IndexModel> findByLemmaId(LemmaModel lemmaId);
}
