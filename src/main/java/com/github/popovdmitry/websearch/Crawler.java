package com.github.popovdmitry.websearch;

import com.github.popovdmitry.websearch.repository.Repository;
import com.github.popovdmitry.websearch.service.StatisticsService;
import com.github.popovdmitry.websearch.utils.Tables;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Crawler {

    private final Repository repository;
    private final StatisticsService statisticsService;
    private final List<String> filter;
    private final Integer delay;
    private final Integer n;
    private final Integer statisticsCollectionIntervalPages;
    private final Integer pagesLimit;

    record Url(Integer fromUrlId, String url, String urlText) {}

    public Crawler(String[] filter, Integer pagesLimit, Integer delay, Boolean loggingEnable,
                   Boolean dbInsertingEnable, Integer n, Integer statisticsCollectionIntervalPages) throws SQLException {
        this.filter = List.of(filter);
        this.pagesLimit = pagesLimit;
        this.delay = delay;
        this.repository = new Repository();
        this.statisticsService = new StatisticsService(loggingEnable, dbInsertingEnable, repository);
        this.n = n;
        if (statisticsCollectionIntervalPages < 1) {
            this.statisticsCollectionIntervalPages = null;
        } else {
            this.statisticsCollectionIntervalPages = statisticsCollectionIntervalPages;
        }
    }

    public boolean isIndexed(String url) {
        try {
            return repository.isExist(Tables.URL_LIST_TABLE, "url", url);
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
                URL url = new URL(page.url());

                Document document = null;
                try {
                    document = Jsoup.connect(page.url()).get();
                } catch (HttpStatusException ignored) {
                }

                if (Objects.nonNull(document)) {
                    Integer urlId = repository.addUrl(url.toString());

                    if (Objects.nonNull(urlId)) {
                        String[] words = Arrays.stream(
                                        Jsoup.parse(document.html())
                                                .text()
                                                .split("[^a-zA-Zа-яА-Я0-9]+"))
                                .filter((word) -> !word.isBlank() && !filter.contains(word))
                                .toArray(String[]::new);
                        for (int k = 0; k < words.length; k++) {
                            int wordId = repository.addWord(words[k], 0);
                            repository.addWordLocation(wordId, urlId, k);
                        }
                    }

                    if (Objects.nonNull(urlId) && Objects.nonNull(page.fromUrlId())
                            && Objects.nonNull(page.urlText())) {
                        Integer linkBetweenUrlId = repository.addLinkBetweenUrl(page.fromUrlId(), urlId);
                        String[] linkWords = Arrays.stream(page.urlText().split("[^a-zA-Zа-яА-Я0-9]+"))
                                .filter((word) -> !filter.contains(word))
                                .toArray(String[]::new);
                        repository.addLinkText(linkWords, linkBetweenUrlId);
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
        repository.close();
        statisticsService.getSummary(n);
        statisticsService.close();
    }
}
