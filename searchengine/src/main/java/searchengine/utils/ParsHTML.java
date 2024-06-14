package searchengine.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;


public class ParsHTML extends RecursiveAction {

    private static final Set<String> allLinks = new CopyOnWriteArraySet<>();
    private final LemmaService lemmaService;
    private final PageRepository pageRepository;
    private final List<ParsHTML> tasks = new ArrayList<>();
    private final Set<String> linked = new HashSet<>();
    private static final int MAX_DEPTH = 5;
    private final int depth;
    private final String userAgent;
    private final String domainURL;
    private final SiteModel siteModel;
    private int siteCountPages;
    @Autowired
    public ParsHTML(String url, String domainURL, PageRepository pageRepository,
                    int depth, String userAgent, SiteModel siteModel,
                    LemmaService lemmaService, int siteCountPages) throws IOException
    {
        this.lemmaService = lemmaService;
        this.domainURL = domainURL;
        this.pageRepository = pageRepository;
        this.depth = depth;
        this.userAgent = userAgent;
        this.siteModel = siteModel;
        this.siteCountPages = siteCountPages;
        readHTML(url, userAgent);
    }
    public void readHTML(String url, String userAgent) {
        if (!allLinks.isEmpty() && depth == 0) {
            allLinks.clear();
        }
        Document doc = null;
        try {
            doc = Jsoup.connect(url).
                    userAgent(userAgent)
                    .data("username", "lebedevalekskirill@gmail.com")
                    .data("password", "KlaccS987")
                    .cookie("sessionId", "mySessionId")
                    .cookie("userId", "12345")
                    .get();

        } catch (IOException ignored) {}
        if (doc != null) {
            allLinks.add(url);
            linked.add(url);
            Elements elements = doc.select("a[href]");
            PageModel pageModel = new PageModel(siteModel, url, HttpStatus.OK.value(), doc.toString());
            pageRepository.save(pageModel);
            lemmaService.indexPage(pageModel);
            for (Element element : elements) {
                String href = element.attr("abs:href");
                if (!allLinks.contains(href) && !href.contains("#") && (href.endsWith("/") || href.endsWith(".html"))
                        && linked.size() < siteCountPages) {
                    linked.add(href);
                }
            }
        }
    }
    @Override
    protected void compute() {
        if (depth < MAX_DEPTH + 1) {
            try {
                Thread.sleep(300);
                if (!linked.isEmpty()) {
                    for (String link : linked) {
//                        while (allLinks.size() <= COUNT_PAGES) {
                            if (allLinks.add(link)) {
                                System.out.println(link);
                                siteCountPages = siteCountPages - linked.size();
                                ParsHTML task = new ParsHTML(link, domainURL, pageRepository, depth + 1 ,
                                        userAgent, siteModel, lemmaService, siteCountPages);
                                task.fork();
                                tasks.add(task);
                            }
//                        }
                    }
                }
                invokeAll(tasks);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
