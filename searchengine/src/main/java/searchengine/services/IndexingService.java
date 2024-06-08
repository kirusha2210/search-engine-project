package searchengine.services;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
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
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IndexingService {
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final LemmaService lemmaService;
    private static final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 YaBrowser/24.4.0.0 Safari/537.36";
    public static Set<String> allLinks = new CopyOnWriteArraySet<>();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final ArrayList<ForkJoinPool> pools;
    private final ExecutorService executorService;
    private SiteModel siteModel;
    private boolean indexed;
    private final JdbcTemplate jdbcTemplate;


    @Autowired
    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository,
                           SitesList sitesList, ArrayList<ForkJoinPool> pools, LemmaService lemmaService,
                           JdbcTemplate jdbcTemplate) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.sitesList = sitesList;
        this.pools = pools;
        this.lemmaService = lemmaService;
        this.jdbcTemplate = jdbcTemplate;
        this.executorService = Executors.newCachedThreadPool();
    }

    public Massage startIndexing() {
        indexed = true;
        try {
            truncateAllTables();
            for (Site site : sitesList.getSites()) {
                siteModel = siteRepository.findByUrl(site.getUrl()).orElse(null);
                if (siteModel != null) {
                    siteModel.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteModel);
                } else {
                    siteModel = new SiteModel(IndexingStatus.INDEXED, LocalDateTime.now(), "", site.getUrl(), site.getName());
                    siteRepository.save(siteModel);
                }
                executeParsingTask(siteModel, site);
            }
            return new Massage(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            indexed = false;
            String massage = "Индексация нарушена ошибками на внутреннем сервере";
            return new Massage(false, massage);
        }
    }
    private void executeParsingTask(SiteModel siteModel, Site site) {
        pools.add(forkJoinPool);
        executorService.submit(() -> {
            ParsHTML parsHTML = null;
            try {
                parsHTML = new ParsHTML(site.getUrl(), site.getUrl(), pageRepository, 0, userAgent, siteModel, lemmaService);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            forkJoinPool.invoke(parsHTML);
            indexed = false;
        });
    }

    public Massage stopIndexing() {
        String massage = "Индексация прекращена успешно";
        try {
            if (indexed) {
                for (ForkJoinPool forkJoinPool : pools) {
                    forkJoinPool.shutdownNow();
                }
                for (SiteModel model : siteRepository.findAll()) {
                    model.setStatus(IndexingStatus.INDEXING);
                }
                indexed = false;
                pools.clear();
                return new Massage(true);
            } else {
                massage = "Индексация не была запущена";
                return new Massage(false, massage);
            }
        } catch (Exception ex) {
            indexed = true;
            massage = "Прерывание ндексации нарушено ошибками на внутреннем сервере";
            return new Massage(false, massage);
        }
    }
    public Massage pageIndexing(String url) throws IOException {
        indexed = true;
        Matcher matcher;
        String error = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";
            matcher = Pattern.compile("https://[^,\\s\"]+").matcher(url);
            if (matcher.find()) {
                url = matcher.group();
            }
            PageModel pageModel = pageRepository.findByPath(url).orElse(null);
            if (pageModel != null) {
                return parsingHtml(pageModel);
            }
            if (processMatchingSites(url)) {
                return new Massage(true);
            }
            indexed = false;
            return new Massage(false, error);
    }

    @Transactional
    public void truncateAllTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE lemma");
        jdbcTemplate.execute("TRUNCATE TABLE page");
        jdbcTemplate.execute("TRUNCATE TABLE site");
        jdbcTemplate.execute("TRUNCATE TABLE index_table");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private boolean processMatchingSites(String url) throws IOException {
        URL siteUrl = null;
        URL pageUrl = null;
        for (Site site : sitesList.getSites()) {
            siteUrl = new URL(site.getUrl());
            pageUrl = new URL(url);
            if (siteUrl.getHost().contains(pageUrl.getHost())) {
                parsingHtml(site, url);
                return true;
            }
        }
        return false;
    }

    public Massage parsingHtml(PageModel pageModel) throws IOException {
        pools.add(forkJoinPool);
        ParsHTML parsHTML = new ParsHTML(pageModel.getPath(), pageModel.getSiteId().getUrl(),
                pageRepository, 0, userAgent, pageModel.getSiteId(), lemmaService);
        forkJoinPool.invoke(parsHTML);
        return new Massage(true, "Индексация успешна");
    }

    public void parsingHtml(Site site, String url) throws IOException {
        pools.add(forkJoinPool);
        SiteModel siteModel = siteRepository.findByName(site.getName())
                .orElse(new SiteModel(IndexingStatus.INDEXED, LocalDateTime.now(), "", site.getUrl(), site.getName()));
        siteRepository.save(siteModel);
        ParsHTML parsHTML = new ParsHTML(url, site.getUrl(),
                pageRepository, 0, userAgent, siteModel, lemmaService);
        forkJoinPool.invoke(parsHTML);
        new Massage(true, "Индексация успешна");
    }

}
