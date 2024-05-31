package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Integer> {
//    SiteModel findByName(String name);
    boolean existsByName(String name);
    Optional<SiteModel> findByName(String name);
    Optional<SiteModel> findByUrl(String url);
}
