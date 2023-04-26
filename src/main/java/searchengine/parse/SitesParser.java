package searchengine.parse;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.SitesList;

//@Component
//@Scope
public class SitesParser extends RecursiveTask<Set<String>> {
    SitesList sitesList = new SitesList();
    public String url;
    private static final Set<String> linksCollection = Collections.synchronizedSet(new HashSet<>());
    private static int count;
    TreeSet<String> result = new TreeSet<>();
    private final int tabsCount;

    public SitesParser(String url, int tabsCount) {
        this.tabsCount = tabsCount;
        this.url = url;
    }
    @Override
    protected Set<String> compute() {
        List<String> links = new ArrayList<>(parseLinks(url));
        List<SitesParser> list = new ArrayList<>();

        for (String child : links) {
            SitesParser task = new SitesParser(child, tabsCount + 1);
            list.add(task);
            task.fork();
        }
        for (SitesParser task : list) {
            linksCollection.addAll(task.join());
        }
        return linksCollection;
    }
    private String getTab(int count) {
        return "\t".repeat(Math.max(0, count));
    }

    public Set<String> parseLinks(String url) {
        result.clear();
        try {
            Thread.sleep(100);
            Document document = Jsoup.connect(url).maxBodySize(0)
                    .userAgent("Mozilla/5.0").referrer("http://www.google.com/")
                    .ignoreContentType(true).ignoreHttpErrors(true).get();
            Elements element = document.select("a");
            for (Element links : element) {
                String linkUrl = links.attr("href");
                if (!linkUrl.endsWith("/") ) {
                    continue;
                }
                String tc = getTab(tabsCount).concat(linkUrl);

                if (linksCollection.add(tc)) {
                    result.add(tc);
                    count++;
                    if (count == 100) {
                        System.out.println("\t Количество ссылок: \t" +
                                linksCollection.size());
                        count = 0;
                    }
                }
            }
            result.remove(url);
        } catch (HttpStatusException httpEx) {
            result.remove(url);
        } catch (IOException sockEx) {
            System.out.println(sockEx.getLocalizedMessage());
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
        return result;
    }
}
