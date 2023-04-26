package searchengine.parse;

import org.jsoup.Connection;
import searchengine.config.Site;
import searchengine.model.*;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapParser extends RecursiveTask<Integer> {
    private static final List<String> WRONG_TYPES = Arrays.asList("jpg", "jpeg", "pdf", "png", "gif", "zip",
            "tar", "jar", "gz", "svg", "ppt", "pptx");

    static {
        websites = new CopyOnWriteArraySet<>();

    }

    private static final Set<String> websites;

    private static AtomicInteger pageId;
    private String mainPage = "";

    private static PageRepository pageRepository;
    private static SiteRepository siteRepository;
    private static Config config;

    private Integer pageCount;
    private final List<MapParser> children;
    private String startPage;
    private final SiteModel site;


    public MapParser(String startPage, SiteModel site, String mainPage) {
        children = new ArrayList<>();

        this.startPage = startPage;
        websites.add(startPage);
        pageCount = 0;

        if (this.mainPage.equals("")) {
            this.mainPage = mainPage;
        }

        this.site = site;

    }

    public MapParser(String startPage, SiteModel site, Config config,
                     SiteRepository siteRepository, PageRepository pageRepository) {
        children = new ArrayList<>();

        this.startPage = startPage;
        websites.add(startPage);
        websites.add(startPage + "/");
        pageCount = 0;

        if (mainPage.equals("")) {
            mainPage = startPage;
        }


        if (MapParser.pageRepository == null) {
            MapParser.pageRepository = pageRepository;
        }

        if (MapParser.siteRepository == null) {
            MapParser.siteRepository = siteRepository;
        }

        this.site = site;

        MapParser.config = config;

        MapParser.pageId = new AtomicInteger(0);
    }

    @Override
    protected Integer compute() {
        if (checkType(startPage)) {
            try {
                if (!startPage.endsWith("/")) {
                    startPage += "/";
                }
                synchronized (pageId) {
                    pageId.getAndIncrement();

                    Connection.Response response = Jsoup.connect(startPage)
                            .ignoreHttpErrors(true)
                            .userAgent(config.getUserAgent())
                            .referrer(config.getReferrer())
                            .execute();

                    Document document = response.parse();

                    Thread.sleep(1000);

                    addPage(response, document);

                    Elements elements = document.select("a");
                    elements.forEach(element -> {
                        String attr = element.attr("href");
                        if (!attr.contains("http")) {
                            if (!attr.startsWith("/") && attr.length() > 1) {
                                attr = "/" + attr;
                            }

                            attr = mainPage + attr;
                        }

                        if (attr.contains(mainPage) && !websites.contains(attr) && !attr.contains("#")) {
                            newChild(attr);
                        }
                    });
                }

            } catch (InterruptedException | NullPointerException | IOException exception) {
                site.setLastError("Остановка индексации");
                site.setStatus(SiteStatus.FAILED);
                siteRepository.save(site);
            }

            children.forEach(it -> {
                pageCount += it.join();
            });
        }

        return pageCount;
    }

    public void addPage() throws IOException {
        Connection.Response response = Jsoup.connect(startPage)
                .userAgent(config.getUserAgent())
                .referrer(config.getReferrer())
                .ignoreHttpErrors(true)
                .execute();

        addPage(response, response.parse());
    }

    private void newChild(String attr) {
        websites.add(attr);

        MapParser newChild = new MapParser(attr, site, mainPage);
        newChild.fork();
        children.add(newChild);
    }

    private void addPage(Connection.Response response, Document document) {
        Page page = pageRepository.findByPath(startPage);
        if (page == null) {
            page = new Page();
        }

        page.setCode(response.statusCode());
        page.setPath(startPage);
        page.setContent(document.html());
        page.setSiteId(site);

        pageRepository.save(page);

       /* if (response.statusCode() < 400) {
            addLemmas(document, page);
        }*/
    }

    /*private void addLemmas(Document document, Page page) {
        AtomicBoolean newPage = new AtomicBoolean(true);
        fields.forEach((key, value) -> {
            Elements el = document.select(key);
            lemmatizer.addString(el.text(), newPage.get(), value, site);
            newPage.set(false);
        });

        addIndexTable(page);
    }*/

   /* private void addIndexTable(Page page) {
        lemmatizer.getLemmasWithRanks().forEach((lemma, rank) -> {
            IndexTable indexTable = new IndexTable();
            indexTable.setLemma(lemma);
            indexTable.setPage(page);
            indexTable.setLemmaRank(rank);
            searchIndexRepository.save(indexTable);
        });
    }*/

    private boolean checkType(String pathPage) {
        return !WRONG_TYPES.contains(pathPage.substring(pathPage.lastIndexOf(".") + 1));
    }

    public SiteModel getSite() {
        return site;
    }
}
