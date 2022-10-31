package com.github.popovdmitry.websearch;

import com.github.popovdmitry.websearch.exception.NotFoundException;
import com.github.popovdmitry.websearch.service.CrawlerService;
import com.github.popovdmitry.websearch.service.SearchService;
import com.github.popovdmitry.websearch.utils.ConfigUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            CrawlerService crawlerService = new CrawlerService(
                    ConfigUtils.getProperty("CRAWLER.FILTER").split(","),
                    ConfigUtils.getIntegerProperty("CRAWLER.PAGES_LIMIT"),
                    ConfigUtils.getIntegerProperty("CRAWLER.DELAY"),
                    ConfigUtils.getBooleanProperty("CRAWLER.LOGGING_ENABLE"),
                    ConfigUtils.getBooleanProperty("CRAWLER.DB_INSERTING_ENABLE"),
                    ConfigUtils.getIntegerProperty("CRAWLER.TOP_N"),
                    ConfigUtils.getIntegerProperty("CRAWLER.STATISTICS_COLLECTION_INTERVAL_PAGES")
            );
            crawlerService.crawl(
                    List.of(ConfigUtils.getProperty("CRAWLER.PAGES").split(",")),
                    Integer.valueOf(ConfigUtils.getProperty("CRAWLER.DEPTH"))
            );
        } catch (SQLException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        try {
            SearchService searchService = new SearchService(ConfigUtils.getProperty("SEARCHER.FILE_PATH"));
            System.out.println(searchService.getSortedMap("python json", 3).toString());
        } catch (SQLException | NotFoundException e) {
            e.printStackTrace();
        }
    }
}
