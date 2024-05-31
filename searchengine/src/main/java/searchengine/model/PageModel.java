package searchengine.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Table(name = "page", indexes = {@Index(name = "path_index", columnList = "path")})
@Getter
@Setter
@Entity
@NoArgsConstructor
public class PageModel {
    public PageModel(SiteModel siteId, String path, int code, String content) {
        this.siteId = siteId;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne()
    @JoinColumn(name = "site_id", nullable = false)
    private SiteModel siteId;
//    INT NOT NULL — ID веб-сайта из таблицы site;

    @Column(name = "path", nullable = false)
    private String path;
//    TEXT NOT NULL — адрес страницы от корня сайта (должен начинаться со слэша, например: /news/372189/);

    @Column(name = "code", nullable = false)
    private int code;
//    INT NOT NULL — код HTTP-ответа, полученный при запросе страницы (например, 200, 404, 500 или другие);

    @Column(name = "context", columnDefinition = "MEDIUMTEXT not null")
    private String content;
//    MEDIUMTEXT NOT NULL — контент страницы (HTML-код).

    @OneToMany(mappedBy = "pageId", cascade = CascadeType.ALL, orphanRemoval = true )
    @Column(name = "indexes")
    private List<IndexModel> indexModelList;
}
