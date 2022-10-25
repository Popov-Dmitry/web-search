package com.github.popovdmitry.websearch;

import com.github.popovdmitry.websearch.utils.ConfigUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            Crawler crawler = new Crawler(
                    ConfigUtils.getProperty("FILTER").split(","),
                    ConfigUtils.getIntegerProperty("PAGES_LIMIT"),
                    ConfigUtils.getIntegerProperty("DELAY"),
                    ConfigUtils.getBooleanProperty("LOGGING_ENABLE"),
                    ConfigUtils.getBooleanProperty("DB_INSERTING_ENABLE"),
                    ConfigUtils.getIntegerProperty("TOP_N"),
                    ConfigUtils.getIntegerProperty("STATISTICS_COLLECTION_INTERVAL_PAGES")
            );
            crawler.crawl(
                    List.of(ConfigUtils.getProperty("PAGES").split(",")),
                    Integer.valueOf(ConfigUtils.getProperty("DEPTH"))
            );
        } catch (SQLException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
