package searchengine.services;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.Massage;
import searchengine.model.IndexingStatus;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.LemmaService;
import searchengine.utils.ParsHTML;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IndexingService {
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final LemmaService lemmaService;
    private static final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 YaBrowser/24.1.0.0 Safari/537.36";
    public static Set<String> allLinks = new CopyOnWriteArraySet<>();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final ArrayList<ForkJoinPool> pools;
    private Matcher matcher;
    private boolean indexed;


    @Autowired
    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository,
                           SitesList sitesList, ArrayList<ForkJoinPool> pools, LemmaService lemmaService) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.sitesList = sitesList;
        this.pools = pools;
        this.lemmaService = lemmaService;
        matcher = null;
    }

    public Massage startIndexing() {
        String massage = "Индексация успешна";
        SiteModel siteModel;
        try {
            for (Site site : sitesList.getSites()) {
                if (!siteRepository.existsByName(site.getName())) {
                    siteModel = new SiteModel(IndexingStatus.INDEXED, LocalDateTime.now(), "", site.getUrl(), site.getName());
                    pools.add(forkJoinPool);
                    siteRepository.save(siteModel);
                    searchengine.utils.ParsHTML parsHTML = new ParsHTML(site.getUrl(), site.getUrl(), pageRepository, 0, userAgent, siteModel, lemmaService);
                    forkJoinPool.invoke(parsHTML);
                }
//                else {
//                    massage = "Индексация уже была запущена";
//                    return new ResponseEntity<>(new Massage(false, massage), HttpStatus.BAD_REQUEST);
//                }
            }
            return new Massage(true);
//            return new ResponseEntity<>(new Massage(true, massage), HttpStatus.OK);
        } catch (Exception ex) {
            ex.printStackTrace();
            indexed = false;
            massage = "Индексация нарушена ошибками на внутреннем сервере";
            return new Massage(false, massage);
        }
    }

    public Massage stopIndexing() {
        String massage = "Индексация прекращена успешно";
        try {
            if (indexed) {
                for (SiteModel model : siteRepository.findAll()) {
                    model.setStatus(IndexingStatus.INDEXING);
                }
                indexed = false;
                for (ForkJoinPool forkJoinPool : pools) {
                    forkJoinPool.shutdownNow();
                }
                pools.clear();
                return new Massage(true);
            } else {
                massage = "Индексация не была запущена";
                return new Massage(false, massage);
            }
//TODO: убрать статус код в респонзе, вернуть massage
        } catch (Exception ex) {
            indexed = true;
            massage = "Прерывание ндексации нарушено ошибками на внутреннем сервере";
            return new Massage(false, massage);
        }
    }
    public ResponseEntity<Massage> pageIndexing(String url) throws IOException {
        PageModel pageModel = pageRepository.findPageByPath(url);
        String regex = "^(https?://[^/]+)";
        matcher = Pattern.compile(regex).matcher(url);

        if (pageModel != null) {
            return parsingHtml(pageModel);
        }

        if (matcher.find() && processMatchingSites(matcher.group(1), url)) {
            return new ResponseEntity<>(new Massage(true, "Индексация успешна"), HttpStatus.OK);
        }

        return new ResponseEntity<>(new Massage(false, "Данной страницы не существует в рамках конфигурационного файла"), HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<Massage> deleteByPage(String url) throws IOException {
        PageModel pageModel = pageRepository.findPageByPath(url);
        String regex = "^(https?://[^/]+)";
        matcher = Pattern.compile(regex).matcher(url);

        if (pageModel != null) {
            pageRepository.delete(pageModel);
        }
        return new ResponseEntity<>(new Massage(true, "Удаление успешно"), HttpStatus.OK);
    }

    private boolean processMatchingSites(String domain, String url) throws IOException {
        for (Site site : sitesList.getSites()) {
            if (site.getUrl().contains(domain)) {
                parsingHtml(site, url);
                return true;
            }
        }
        return false;
    }

    public ResponseEntity<Massage> parsingHtml(PageModel pageModel) throws IOException {
        pools.add(forkJoinPool);
        ParsHTML parsHTML = new ParsHTML(pageModel.getPath(), pageModel.getSiteId().getUrl(),
                pageRepository, 0, userAgent, pageModel.getSiteId(), lemmaService);
        forkJoinPool.invoke(parsHTML);
        return new ResponseEntity<>(new Massage(true, "Индексация успешна"),HttpStatus.OK);
    }

    public void parsingHtml(Site site, String url) throws IOException {
        pools.add(forkJoinPool);
        SiteModel siteModel = siteRepository.findByName(site.getName())
                .orElse(new SiteModel(IndexingStatus.INDEXED, LocalDateTime.now(), "", site.getUrl(), site.getName()));
        siteRepository.save(siteModel);
        ParsHTML parsHTML = new ParsHTML(url, site.getUrl(),
                pageRepository, 0, userAgent, siteModel, lemmaService);
        forkJoinPool.invoke(parsHTML);
        new ResponseEntity<>(new Massage(true, "Индексация успешна"), HttpStatus.OK);
    }

}
