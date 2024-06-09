package searchengine.repositories;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    Optional<List<LemmaModel>> findBySiteIdAndLemma(SiteModel siteId, String lemma);
    @Modifying
    @Query(value = "INSERT INTO lemma (site_id, lemma, frequency) " +
            "VALUES (:siteId, :lemma, 1) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + 1", nativeQuery = true)
    void upsertLemma(@Param("siteId") int siteId, @Param("lemma") String lemma);
}
