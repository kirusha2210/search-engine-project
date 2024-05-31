package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.Massage;
import searchengine.dto.SearchData;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.LemmaService;

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
        float relevance = 0;
        String[] snippet = new String[0];
        SearchData data = new SearchData(siteUrl, siteModel.getName(), siteModel.getUrl(), snippet, relevance);
        return new Massage(true, data, relevantPageList.size());
    }
}
