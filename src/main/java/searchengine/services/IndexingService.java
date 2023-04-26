package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.IndexResponse;
import searchengine.model.PageRepository;
import searchengine.model.SiteModel;
import searchengine.model.SiteRepository;
import searchengine.model.SiteStatus;
import searchengine.parse.Config;
import searchengine.parse.MapParser;
import searchengine.parse.SitesParser;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingService {
    private static final int NUMBER_OF_THREADS = 2;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;
    private IndexResponse indexResponse;
    private boolean isIndexing;
    private List<Thread> threads = new ArrayList<>();
    private List<ForkJoinPool> forkJoinPools = new ArrayList<>();

    @Autowired
    private Config config;

    private int tabs = 0;


   /* private SitesParser newParse(Site site) {
        return new SitesParser(site.getUrl(), tabs + 1);
    }*/
    public void clearAll() {
        pageRepository.deleteAll();
        siteRepository.deleteAll();
    }
    private MapParser newParse(SiteModel site) {
        return new MapParser(site.getUrl(), site, config, siteRepository, pageRepository);
    }

    public void indexing() {
        threads = new ArrayList<>();
        forkJoinPools = new ArrayList<>();

        clearAll();
        //Lemmatizer.setLemmaRepository(lemmaRepository);

        List<MapParser> parses = new ArrayList<>();
        List<String> urls = config.getUrl();
        List<String> namesUrls = config.getName();
        SiteModel site;


        for (int i = 0; i < urls.size(); ++i) {
            String mainPage = urls.get(i);

            site = siteRepository.findSiteByUrl(mainPage);

            //site = new SiteModel();

            site.setUrl(mainPage);
            site.setStatusTime(new Date().toInstant());
            site.setStatus(SiteStatus.INDEXING);
            site.setSiteName(namesUrls.get(i));

            siteRepository.save(site);
            System.out.println("Site added");
            parses.add(newParse(site));
            System.out.println("Site parsed");
        }

        urls.clear();
        namesUrls.clear();


        parses.forEach(parse -> threads.add(new Thread(() -> {
            SiteModel siteTh = parse.getSite();

            try {
                siteTh.setStatus(SiteStatus.INDEXING);
                siteRepository.save(siteTh);

                ForkJoinPool forkJoinPool = new ForkJoinPool(NUMBER_OF_THREADS);

                forkJoinPools.add(forkJoinPool);

                forkJoinPool.execute(parse);
                int count = parse.join();

                siteTh.setStatus(SiteStatus.INDEXED);
                siteRepository.save(siteTh);


                System.out.println("Сайт " + siteTh.getSiteName() + " проиндексирован,кол-во ссылок - " + count);
            } catch (CancellationException ex) {
                ex.printStackTrace();
                siteTh.setLastError("Ошибка индексации: " + ex.getMessage());
                siteTh.setStatus(SiteStatus.FAILED);
                siteRepository.save(siteTh);
            }
        })));

        threads.forEach(Thread::start);
        forkJoinPools.forEach(ForkJoinPool::shutdown);

        forkJoinPools.forEach(ForkJoinPool::shutdown);
    }

    public void parsingLinks() {
        int cores = Runtime.getRuntime().availableProcessors();
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        SitesParser sitesParser = new SitesParser(config.getUrl().toString(), tabs + 1);
        List<String> map = new ArrayList<>(forkJoinPool.invoke(sitesParser));
        SiteModel siteModel = new SiteModel();
        siteModel.setSiteName(map.get(1));
        siteRepository.save(siteModel);
    }

    public boolean startIndexing() {
        AtomicBoolean isIndexing = new AtomicBoolean(false);

        siteRepository.findAll().forEach(site -> {
            if (site.getStatus().equals(SiteStatus.INDEXING)) {
                isIndexing.set(true);
            }
        });

        if (isIndexing.get()) {
            return true;
        }
        new Thread(this::parsingLinks).start();

        return false;
    }

    public boolean stopIndexing() {
        System.out.println("Потоков работает: " + threads.size());

        AtomicBoolean isIndexing = new AtomicBoolean(false);

        siteRepository.findAll().forEach(site -> {
            if (site.getStatus().equals(SiteStatus.INDEXING)) {
                isIndexing.set(true);
            }
        });

        if (!isIndexing.get()) {
            return true;
        }

        forkJoinPools.forEach(ForkJoinPool::shutdownNow);
        threads.forEach(Thread::interrupt);

        siteRepository.findAll().forEach(site -> {
            site.setLastError("Остановка индексации");
            site.setStatus(SiteStatus.FAILED);
            siteRepository.save(site);
        });

        threads.clear();
        forkJoinPools.clear();

        return false;
    }



    /*public IndexResponse stopIndexing() {
        indexResponse.setResult(false);
        indexResponse.setError("Индексация уже запущена");
        return indexResponse;
    }*/
}
