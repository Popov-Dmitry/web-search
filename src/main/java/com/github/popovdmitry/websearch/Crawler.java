package com.github.popovdmitry.websearch;

import com.github.popovdmitry.websearch.repository.Repository;
import com.github.popovdmitry.websearch.utils.ConfigUtils;
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
    private final String URL_LIST_TABLE = ConfigUtils.getProperty("URL_LIST_TABLE");
    private final List<String> filter;
    private final Integer delay;

    record Url(Integer fromUrlId, String url, String urlText) {}

    public Crawler(String[] filter, Integer delay) throws SQLException {
        this.filter = List.of(filter);
        this.delay = delay;
        repository = new Repository();
    }

    public boolean isIndexed(String url) {
        try {
            return repository.isExist(URL_LIST_TABLE, "url", url);
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void crawl(List<String> initialPages, Integer depth) throws IOException, SQLException, InterruptedException {
        List<Url> pages = initialPages.stream().map((page) -> new Url(null, page, null)).toList();
        for(int i = 0; i < depth; i++) {
            List<Url> newPagesList = new ArrayList<>();

            for(int j = 0; j < pages.size(); j++) {
                if (Objects.nonNull(delay)) {
                    TimeUnit.SECONDS.sleep(delay);
                }
                System.out.println(j + " " + pages.size());
                URL page = new URL(pages.get(j).url());

                Document document = null;
                try {
                    document = Jsoup.connect(pages.get(j).url()).get();
                } catch (HttpStatusException ignored) {}

                if (Objects.nonNull(document)) {
                    Integer urlId = repository.addUrl(page.toString());

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

                    if (Objects.nonNull(urlId) && Objects.nonNull(pages.get(j).fromUrlId())
                            && Objects.nonNull(pages.get(j).urlText())) {
                        Integer linkBetweenUrlId =  repository.addLinkBetweenUrl(pages.get(j).fromUrlId(), urlId);
                        String[] linkWords = Arrays.stream(pages.get(j).urlText().split("[^a-zA-Zа-яА-Я0-9]+"))
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
                                        if (page.getPath().equals(link.attr("href"))
                                                || (page.getPath() + "/").equals(link.attr("href"))) {
                                            unifiedLink = page.toString();
                                        } else {
                                            unifiedLink = page + link.attr("href");
                                        }

                                    } else {
                                        unifiedLink = page.toString();
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
            }
            pages = newPagesList;
        }
        repository.close();
    }
}
