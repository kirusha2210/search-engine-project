package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Table(name = "lemma", indexes = {@Index(name = "site_index", columnList = "site_id")}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"site_id", "lemma"})})
@Getter
@Setter
@Entity
@NoArgsConstructor
public class LemmaModel {
    public LemmaModel(SiteModel siteId, String lemma, int frequency) {
        this.siteId = siteId;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne()
    private SiteModel siteId;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemmaId")
    @Column(name = "indexes")
    private List<IndexModel> indexModelList;
}
