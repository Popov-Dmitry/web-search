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
                    100,
                    2,
                    true,
                    true,
                    20,
                    10
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
