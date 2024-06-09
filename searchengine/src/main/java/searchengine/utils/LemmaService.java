package searchengine.utils;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Log4j2
public class LemmaService {
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final LuceneMorphology russianLuceneMorph;
    private final LuceneMorphology englishLuceneMorph;
    private final List<String> skipFormsRussian = Arrays.asList("ПРЕДЛ", "МЕЖД", "СОЮЗ", "ЧАСТ");
    private final List<String> skipFormsEnglish = Arrays.asList("CONJ", "PREP", "INT", "PART");
    private final Map<String, Integer> lemmaToPage = new ConcurrentHashMap<>();
    @Autowired
    public LemmaService(IndexRepository indexRepository, PageRepository pageRepository,
                        LemmaRepository lemmaRepository) throws IOException
    {
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.russianLuceneMorph = new RussianLuceneMorphology();
        this.englishLuceneMorph = new EnglishLuceneMorphology();
    }

    @Transactional
    public void indexPage(PageModel pageModel) {
        String html = pageModel.getContent();
        String text = Jsoup.parse(html).text().toLowerCase();
        if(lemmaToPage.size() >= 1000) { lemmaToPage.clear(); }
        createLemmas(text, lemmaToPage);
        saveLemmasAndIndices(pageModel);
    }
    public void createLemmas(String text, Map<String, Integer> lemmsMap) {
        String russianCleanText = text.replaceAll("[^А-Яа-я\\s]", "");
        createLemmas(russianCleanText, russianLuceneMorph, skipFormsRussian, lemmsMap);

        String englishCleanText = text.replaceAll("[^A-Za-z\\s]", "");
        createLemmas(englishCleanText, englishLuceneMorph, skipFormsEnglish, lemmsMap);
    }
    private void createLemmas(String cleanText, LuceneMorphology luceneMorphology,
                              List<String> skipForms, Map<String, Integer> lemmsMap)
    {
        Arrays.stream(cleanText.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .forEach(word -> {
                    String[] morphForm;
                    String normalForm;
                    List<String> morphInfos = luceneMorphology.getMorphInfo(word);
                    if (!morphInfos.isEmpty()) {
                        morphForm = morphInfos.get(0).split(" ");
                        if (morphForm.length > 1 && !skipForms.contains(morphForm[1])) {
                            normalForm = luceneMorphology.getNormalForms(word).get(0);
                            lemmsMap.merge(normalForm, 1, Integer::sum);
                        }
                    }
                });
    }
    public Map<String, Integer> searchLemmas (String cleanText) {
        return lemmaToPage.entrySet().stream()
                .filter(entry -> cleanText.contains(entry.getKey()))
                .filter(entry -> entry.getValue() < 10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Transactional
    private synchronized void saveLemmasAndIndices(PageModel pageModel) {
        Set<LemmaModel> lemmaModels = new HashSet<>();
        Set<IndexModel> indexModels = new HashSet<>();

        lemmaToPage.forEach((lemma, countOnPage) -> {
            lemmaRepository.upsertLemma(pageModel.getSiteId().getId(), lemma);
            indexModels.add(new IndexModel(pageModel,
                    lemmaRepository.findBySiteIdAndLemma(pageModel.getSiteId(), lemma).orElseThrow().get(0),
                    countOnPage));
        });
        System.out.println("List completed");
        try {
            indexRepository.saveAll(indexModels);
            if (indexModels.size() >= 1000) { indexModels.clear(); }
            if(lemmaToPage.size() >= 1000) { lemmaToPage.clear(); }
        } catch (Exception e) {
            log.error("Error while processing page: " + pageModel.getId(), e);
        }
    }
}
