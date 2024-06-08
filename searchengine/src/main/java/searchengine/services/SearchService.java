package searchengine.services;

import lombok.RequiredArgsConstructor;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaService lemmaService;
    private final List<RelevantPage> relevantPages;
    private final List<SearchData> searchDataList;
    private final List<SearchData> resultData;
    private PageModel pageModel;
    private String snippet = null;
    private Document doc;
    private String docText;
    private int lemmaPosition;


    public Massage search(String query, String siteUrl, int offset, int limit) {
        SiteModel siteModel = siteRepository.findByUrl(siteUrl).orElseThrow();
        String cleanQuery = query.replaceAll("\\p{Punct}", "").toLowerCase();
        Map<String, Integer> searchLemmsMap = lemmaService.searchLemmas(cleanQuery);

        List<LemmaModel> lemmaModels = searchLemmsMap.keySet().stream()
                .map(integer -> lemmaRepository.findBySiteIdAndLemma(siteModel, integer).orElseThrow())
                .sorted(Comparator.comparingInt(LemmaModel::getFrequency))
                .toList();

        Set<PageModel> relevantPageList = lemmaModels.stream()
                .map(lemmaModel -> lemmaModel.getIndexModelList()
                        .stream().map(IndexModel::getPageId)
                        .collect(Collectors.toSet()))
                .reduce((set1, set2) -> {
                    set1.retainAll(set2);
                    return set1;
                })
                .orElse(Collections.emptySet());

        Map<PageModel, Float> absolutRelevances = new HashMap<>();


        relevantPageList.forEach(pageModel -> {
            lemmaModels
                    .stream().map(lemmaModel ->
                            lemmaModel.getIndexModelList()
                                                .stream().map(indexModel ->
                                                        indexRepository.findRankByLemmaIdAndPageId(lemmaModel, pageModel))
                                .reduce((rank1, rank2) -> rank1 + rank2).orElseThrow())
                    .reduce((lemmaRank1, lemmaRank2) -> lemmaRank1 + lemmaRank2)
                    .ifPresent(lemmaRank -> absolutRelevances.put(pageModel, lemmaRank));
        });

        float maxRelevance = absolutRelevances.entrySet()
                .stream().max(Map.Entry.comparingByValue()).orElseThrow().getValue();

        absolutRelevances.forEach((pageModel, absolutRelevance) ->
                relevantPages.add(new RelevantPage(
                pageModel, absolutRelevance/maxRelevance, absolutRelevance)));



        relevantPages.forEach(relevantPage -> {
            pageModel = relevantPage.getPageModel();
            doc = Jsoup.parse(pageModel.getContent());
            lemmaModels.forEach(lemmaModel -> {
                docText = doc.text();
                lemmaPosition = docText.indexOf(lemmaModel.getLemma());
                snippet = docText.substring(lemmaPosition - 100, lemmaPosition + 100);
            });
                searchDataList.add(new SearchData(siteModel.getUrl(), siteModel.getName(),
                        pageModel.getPath(), doc.title(), snippet, relevantPage.getRelativeRelevance()));

        });

//        limit = limit == 0 ? 20 : limit;
//        offset = offset == 0 ? 20 : limit;
        for (;offset < limit; offset++) {
            resultData.add(searchDataList.get(offset));
        }

        return new Massage(true, resultData, searchDataList.size());
    }

}
