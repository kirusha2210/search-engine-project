package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@Table(name = "index_table", uniqueConstraints = @UniqueConstraint(columnNames = {"lemma_id", "page_id"}),
        indexes = {@Index(name = "page_index", columnList = "page_id")})
@Entity
public class IndexModel {
    public IndexModel(PageModel pageId, LemmaModel lemmaId, float rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;
//    INT NOT NULL AUTO_INCREMENT;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "page_id", nullable = false)
    private PageModel pageId;
//    INT NOT NULL — идентификатор страницы;

    @ManyToOne()
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaModel lemmaId;
//    INT NOT NULL — идентификатор леммы;

    @Column(name = "`rank`", columnDefinition = "FLOAT", nullable = false)
    private float rank;
//    FLOAT NOT NULL — количество данной леммы для данной страницы.
}

