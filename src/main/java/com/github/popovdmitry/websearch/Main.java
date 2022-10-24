package com.github.popovdmitry.websearch;

import com.github.popovdmitry.websearch.utils.ConfigUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        Crawler crawler = new Crawler();
        try {
            crawler.crawl(
                    List.of(ConfigUtils.getProperty("PAGES").split(",")),
                    Integer.valueOf(ConfigUtils.getProperty("DEPTH"))
            );
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
