package searchengine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "site")
@NoArgsConstructor
public class SiteModel {

    public SiteModel(IndexingStatus status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
//    NOT NULL AUTO_INCREMENT;
    private int id;

    @Column(name = "status", columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private IndexingStatus status;

    @Column(name = "status_time", columnDefinition = "DATETIME", nullable = false)
    @NotNull
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT", nullable = false)
    @NotNull
    private String lastError;

    @Column(name = "url", columnDefinition = "VARCHAR(255)", nullable = false)
    @NotNull
    private String url;

    @Column(name = "name", columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    @NotNull
    private String name;

    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL)
    @Column(name = "lemmas", nullable = false)
    private List<LemmaModel> lemmaModelList;

    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL)
    @Column(name = "pages", nullable = false)
    private List<PageModel> pageModelList;
}