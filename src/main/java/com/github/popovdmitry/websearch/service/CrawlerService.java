package com.github.popovdmitry.websearch.service;

import com.github.popovdmitry.websearch.repository.CrawlerRepository;
import com.github.popovdmitry.websearch.utils.Tables;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CrawlerService {

    private final CrawlerRepository crawlerRepository;
    private final StatisticsService statisticsService;
    private final List<String> filter;
    private final Integer delay;
    private final Integer n;
    private final Integer statisticsCollectionIntervalPages;
    private final Integer pagesLimit;

    record Url(Integer fromUrlId, String url, String urlText) {}

    public CrawlerService(String[] filter, Integer pagesLimit, Integer delay, Boolean loggingEnable,
                          Boolean dbInsertingEnable, Integer n, Integer statisticsCollectionIntervalPages) throws SQLException {
        this.filter = List.of(filter);
        this.pagesLimit = pagesLimit;
        this.delay = delay;
        this.crawlerRepository = new CrawlerRepository();
        this.statisticsService = new StatisticsService(loggingEnable, dbInsertingEnable, crawlerRepository);
        this.n = n;
        if (statisticsCollectionIntervalPages < 1) {
            this.statisticsCollectionIntervalPages = null;
        } else {
            this.statisticsCollectionIntervalPages = statisticsCollectionIntervalPages;
        }
    }

    public boolean isIndexed(String url) {
        try {
            return crawlerRepository.isExist(Tables.URL_LIST_TABLE, "url", url);
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void crawl(List<String> initialPages, Integer depth) throws IOException, SQLException, InterruptedException {
        List<Url> pages = initialPages.stream()
                .map((page) -> new Url(null, page, null))
                .toList();
        int pagesProcessedCount = 0;
        for(int i = 0; i < depth; i++) {
            List<Url> newPagesList = new ArrayList<>();
            if (Objects.nonNull(pagesLimit) && pages.size() > pagesLimit) {
                pages = pages.stream().limit(pagesLimit).toList();
            }

            for (Url page : pages) {
                if (Objects.nonNull(delay)) {
                    TimeUnit.SECONDS.sleep(delay);
                }
                URL url;
                try {
                     url = new URL(page.url());
                } catch (MalformedURLException e) {
                    continue;
                }


                Document document = null;
                try {
                    document = Jsoup.connect(page.url()).get();
                } catch (HttpStatusException ignored) {
                }

                if (Objects.nonNull(document)) {
                    Integer urlId = crawlerRepository.addUrl(url.toString());

                    if (Objects.nonNull(urlId)) {
                        String[] words = Arrays.stream(
                                        Jsoup.parse(document.html())
                                                .text()
                                                .split("[^a-zA-Zа-яА-Я0-9]+"))
                                .filter((word) -> !word.isBlank() && !filter.contains(word))
                                .toArray(String[]::new);
                        for (int k = 0; k < words.length; k++) {
                            int wordId = crawlerRepository.addWord(words[k], 0);
                            crawlerRepository.addWordLocation(wordId, urlId, k);
                        }
                    }

                    if (Objects.nonNull(urlId) && Objects.nonNull(page.fromUrlId())
                            && Objects.nonNull(page.urlText())) {
                        Integer linkBetweenUrlId = crawlerRepository.addLinkBetweenUrl(page.fromUrlId(), urlId);
                        String[] linkWords = Arrays.stream(page.urlText().split("[^a-zA-Zа-яА-Я0-9]+"))
                                .filter((word) -> !filter.contains(word))
                                .toArray(String[]::new);
                        crawlerRepository.addLinkText(linkWords, linkBetweenUrlId);
                    }

                    Elements links = document.select("a[href]");
                    links.stream()
                            .filter((link)
                                    -> link.hasText()
                                    && !link.attr("href").isBlank()
                                    && !link.attr("href").startsWith("#")
                            )
                            .forEach((link) -> {
                                String unifiedLink;
                                if (link.attr("href").startsWith("/")) {
                                    if (!link.attr("href").equals("/")) {
                                        if (url.getPath().equals(link.attr("href"))
                                                || (url.getPath() + "/").equals(link.attr("href"))) {
                                            unifiedLink = url.toString();
                                        } else {
                                            unifiedLink = url + link.attr("href");
                                        }

                                    } else {
                                        unifiedLink = url.toString();
                                    }
                                } else {
                                    unifiedLink = link.attr("href");
                                }

                                if (unifiedLink.endsWith("/")) {
                                    unifiedLink = unifiedLink.substring(0, unifiedLink.length() - 1);
                                }
                                if (!isIndexed(unifiedLink)) {
                                    newPagesList.add(new Url(urlId, unifiedLink, link.text()));
                                }
                            });
                }

                pagesProcessedCount++;
                if (Objects.nonNull(statisticsCollectionIntervalPages)) {
                    if (statisticsCollectionIntervalPages == 1) {
                        statisticsService.collectRowsCountStatistics(pagesProcessedCount);
                    } else if (pagesProcessedCount % statisticsCollectionIntervalPages == 0) {
                        statisticsService.collectRowsCountStatistics(pagesProcessedCount);
                    }
                }
            }
            pages = newPagesList;
        }
        statisticsService.getSummary(n);
        crawlerRepository.close();
        statisticsService.close();
    }

    public void calculatePageRank(Integer iterations) throws SQLException {
        crawlerRepository.preparePageRankTable();
        ResultSet urls = crawlerRepository.selectAllFrom(Tables.URL_LIST_TABLE);
        double d = 0.85;
        for (int i = 0; i < iterations; i++) {
            while (urls.next()) {
                double pr = 1 - d;
                ResultSet fromUrlIds = crawlerRepository.getDistinctFromUlrId(urls.getInt(1));
                while (fromUrlIds.next()) {
                    ResultSet resultSet = crawlerRepository.selectFromWhere(
                            Tables.PAGE_RANK_TABLE,
                            "url_id",
                            fromUrlIds.getInt(1));
                    resultSet.next();
                    Integer pageRank = resultSet.getInt(3);
                    Integer pageLinksCount = crawlerRepository.selectRowsCountFromWhere(
                            Tables.LINK_BETWEEN_URL_TABLE,
                            "from_url_id",
                            fromUrlIds.getInt(1));
                    pr += d * ((double) (pageRank / pageLinksCount));
                }
                crawlerRepository.updateValueWhere(
                        Tables.PAGE_RANK_TABLE,
                        "score",
                        pr,
                        "url_id",
                        urls.getInt(1));
            }
        }
    }
}
