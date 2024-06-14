package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.Massage;
import searchengine.dto.search.SearchData;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.LemmaService;
import searchengine.dto.RelevantPage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class SearchService {
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaService lemmaService;
    private final List<SearchData> searchDataList;
    private final List<SearchData> resultData;
    private LuceneMorphology russianLuceneMorph;
    private LuceneMorphology englishLuceneMorph;
    private String snippet = null;
    private int lemmaPosition;
    private List<LemmaModel> lemmaModels;
    private int lastLemmaPosition;
    public Massage search(String query, String siteUrl, String offsetStr, String limitStr) {

        clearPreviousResults();

        int offset = Integer.parseInt(offsetStr);
        int limit = Integer.parseInt(limitStr);

        SiteModel siteModel;
        try {
            siteModel = getSiteModel(siteUrl);
        } catch (NoSuchElementException | IndexOutOfBoundsException ex) {
            return handleError("Нет сайта с таким url", ex);
        } catch (Exception ex) {
            return handleError("Ошибка при формировании модели сайта", ex);
        }

        String cleanQuery = cleanQuery(query);
        Map<String, Integer> searchLemmasMap = lemmaService.searchLemmas(cleanQuery, new HashMap<>());

        try {
            lemmaModels = getSortedLemmaModels(searchLemmasMap, siteModel);
        } catch (NoSuchElementException | IndexOutOfBoundsException ex) {
            return handleError("Не найдено искомых слов на данной странице", ex);
        } catch (Exception ex) {
            return handleError("Ошибка при формировании списка лемм", ex);
        }

        Set<PageModel> relevantPageList;
        try {
            relevantPageList = getRelevantPages(lemmaModels);
        } catch (Exception ex) {
            return handleError("Ошибка при формировании релевантных страниц", ex);
        } if (relevantPageList.isEmpty()) {
            return handleError("Не найдено релевантных страниц", null);
        }

        Map<PageModel, Float> absolutRelevances;
        try {
            absolutRelevances = calculateAbsoluteRelevances(lemmaModels, relevantPageList);
        } catch (Exception ex) {
            return handleError("Ошибка при вычислении абсолютной релевантности", ex);
        }

        float maxRelevance = getMaxRelevance(absolutRelevances);

        List<RelevantPage> relevantPages = getRelevantPagesWithRelevance(absolutRelevances, maxRelevance);

        Set<String> snippets = null;
        try {
            snippets = generateSearchData(relevantPages, siteModel);
        } catch (Exception e) {
            return handleError("Ошибка при генерации сниппета", e);
        }

        List<SearchData> resultData = null;
        try {
            resultData = getResultData(snippets, relevantPages, siteModel, offset, limit);
        } catch (Exception ex) {
            handleError("Ошибка при формировании результатов", ex);
        }

        return new Massage(true, resultData, searchDataList.size());
    }

    private void clearPreviousResults() {
        if (!searchDataList.isEmpty()) {
            searchDataList.clear();
            resultData.clear();
        }
    }

    private SiteModel getSiteModel(String siteUrl) {
        return siteRepository.findByUrl(siteUrl).orElseThrow();
    }

    private String cleanQuery(String query) {
        return query.replaceAll("\\p{Punct}", "").toLowerCase();
    }

    private List<LemmaModel> getSortedLemmaModels(Map<String, Integer> searchLemmasMap, SiteModel siteModel) {
        return searchLemmasMap.keySet().stream()
                .map(lemma -> lemmaRepository.findBySiteIdAndLemma(siteModel, lemma).orElseThrow().get(0))
                .sorted(Comparator.comparingInt(LemmaModel::getFrequency))
                .toList();
    }

    private Set<PageModel> getRelevantPages(List<LemmaModel> lemmaModels) {
        return lemmaModels.stream()
                .map(lemmaModel -> lemmaModel.getIndexModelList()
                        .stream().map(IndexModel::getPageId)
                        .collect(Collectors.toSet()))
                .reduce((set1, set2) -> {
                    set1.retainAll(set2);
                    return set1;
                })
                .orElse(Collections.emptySet());
    }

    private Map<PageModel, Float> calculateAbsoluteRelevances(List<LemmaModel> lemmaModels, Set<PageModel> relevantPageList) {
        Map<PageModel, Float> absolutRelevances = new HashMap<>();
        relevantPageList.forEach(pageModel -> {
            float totalRank = lemmaModels.stream()
                    .map(lemmaModel -> lemmaModel.getIndexModelList()
                            .stream().map(IndexModel::getRank)
                            .reduce(Float::sum).orElseThrow())
                    .reduce(Float::sum).orElseThrow();
            absolutRelevances.put(pageModel, totalRank);
        });
        return absolutRelevances;
    }

    private float getMaxRelevance(Map<PageModel, Float> absolutRelevances) {
        return absolutRelevances.entrySet()
                .stream().max(Map.Entry.comparingByValue()).orElseThrow().getValue();
    }

    private List<RelevantPage> getRelevantPagesWithRelevance(Map<PageModel, Float> absolutRelevances, float maxRelevance) {
        List<RelevantPage> relevantPages = new ArrayList<>();
        absolutRelevances.forEach((pageModel, absolutRelevance) ->
                relevantPages.add(new RelevantPage(pageModel, absolutRelevance / maxRelevance, absolutRelevance)));
        return relevantPages;
    }

    private Set<String> generateSearchData(List<RelevantPage> relevantPages, SiteModel siteModel) throws IOException {
        Set<String> snippets = new HashSet<>();
        russianLuceneMorph = new RussianLuceneMorphology();
        englishLuceneMorph = new EnglishLuceneMorphology();

        relevantPages.forEach(relevantPage -> {
            PageModel pageModel = relevantPage.getPageModel();
            Document doc = Jsoup.parse(pageModel.getContent());
            String docText = doc.text().toLowerCase();
            String russianCleanText = docText.replaceAll("[^а-я-\\s]", " ");
            String englishCleanText = docText.replaceAll("[^a-z-\\s]", " ");
            createSnippets(russianLuceneMorph, snippets, russianCleanText, docText);
            createSnippets(englishLuceneMorph, snippets, englishCleanText, docText);
            snippets.forEach(snippet -> searchDataList.add(new SearchData(siteModel.getUrl(), siteModel.getName(),
                    pageModel.getPath(), doc.title(), snippet, relevantPage.getRelativeRelevance())));
        });

        return snippets;
    }

    private void createSnippets (LuceneMorphology luceneMorphology, Set<String> snippets, String cleanText, String docText) {
        lemmaModels.forEach(lemmaModel -> {
            String[] words = cleanText.split("\\s+");
            Arrays.stream(words)
                    .filter(word -> !word.trim().isEmpty())
                    .filter(word -> luceneMorphology.getNormalForms(word).get(0).contains(lemmaModel.getLemma()))
                    .forEach(word -> {
                        lemmaPosition = docText.indexOf(word);
                        lastLemmaPosition = lemmaPosition + word.length();
                        if (lemmaPosition < 100) {
                            snippet = "..." + docText.substring(0, lemmaPosition) +
                                    "<b>" + word + "</b>" +
                                    docText.substring(lastLemmaPosition, lastLemmaPosition + 100) + "...";
                        }
                        snippet = lemmaPosition < 100 ?
                                "..." + docText.substring(0, lemmaPosition) + "<b>" + word + "</b>" +
                                        docText.substring(lastLemmaPosition, lastLemmaPosition + 100) + "..." :
                                "..." + docText.substring(lemmaPosition - 100, lemmaPosition) + "<b>" + word + "</b>" +
                                docText.substring(lastLemmaPosition, lastLemmaPosition + 100) + "...";
                        snippets.add(snippet);
                    });
        });
    }

    private List<SearchData> getResultData(Set<String> snippets, List<RelevantPage> relevantPages, SiteModel siteModel, int offset, int limit) {
        List<SearchData> resultData = new ArrayList<>();
        limit = limit == offset ? limit + 20 : limit;
        if (searchDataList.size() < limit - offset) {
            for (; offset < searchDataList.size(); offset++) {
                resultData.add(searchDataList.get(offset));
            }
        } else {
            for (; offset < limit; offset++) {
                resultData.add(searchDataList.get(offset));
            }
        }
//        snippets.clear();
//        relevantPages.clear();
        return resultData;
    }

    private Massage handleError(String errorMessage, Exception ex) {
        if (ex != null) {
            log.error(errorMessage, ex);
        } else {
            log.error(errorMessage);
        }
        return new Massage(false, errorMessage);
    }
}
