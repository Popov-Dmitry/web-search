package com.github.popovdmitry.websearch.service;

import com.github.popovdmitry.websearch.repository.Repository;
import com.github.popovdmitry.websearch.repository.StatisticsRepository;
import com.github.popovdmitry.websearch.utils.ConfigUtils;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

public class StatisticsService {

    private final String WORD_LIST_TABLE = ConfigUtils.getProperty("WORD_LIST_TABLE");
    private final String URL_LIST_TABLE = ConfigUtils.getProperty("URL_LIST_TABLE");
    private final String WORD_LOCATION_TABLE = ConfigUtils.getProperty("WORD_LOCATION_TABLE");
    private final String LINK_BETWEEN_URL_TABLE = ConfigUtils.getProperty("LINK_BETWEEN_URL_TABLE");
    private final String LINK_WORD_TABLE = ConfigUtils.getProperty("LINK_WORD_TABLE");

    private final Boolean loggingEnable;
    private final Boolean dbInsertingEnable;
    private final Repository repository;
    private final StatisticsRepository statisticsRepository;

    public StatisticsService(Boolean loggingEnable, Boolean dbInsertingEnable, Repository repository) throws SQLException {
        this.loggingEnable = loggingEnable;
        this.dbInsertingEnable = dbInsertingEnable;
        this.repository = repository;
        if (dbInsertingEnable) {
            statisticsRepository = new StatisticsRepository();
        } else {
            statisticsRepository = null;
        }
    }

    public void getSummary() {

    }

    public Map<String, Integer> getRowsCount() throws SQLException {
        Map<String, Integer> rowsCount = new Hashtable<>();
        Integer wordListRows = repository.selectRowsCountFrom(WORD_LIST_TABLE);
        Integer urlListRows = repository.selectRowsCountFrom(URL_LIST_TABLE);
        Integer wordLocationRows = repository.selectRowsCountFrom(WORD_LOCATION_TABLE);
        Integer linkBetweenUrlRows = repository.selectRowsCountFrom(LINK_BETWEEN_URL_TABLE);
        Integer linkWordRows = repository.selectRowsCountFrom(LINK_WORD_TABLE);
        rowsCount.put(WORD_LIST_TABLE, wordListRows);
        rowsCount.put(URL_LIST_TABLE, urlListRows);
        rowsCount.put(WORD_LOCATION_TABLE, wordLocationRows);
        rowsCount.put(LINK_BETWEEN_URL_TABLE, linkBetweenUrlRows);
        rowsCount.put(LINK_WORD_TABLE, linkWordRows);

        if (loggingEnable) {
            System.out.printf("""
                            		 Rows Count
                            %s\t\t\t%d
                            %s\t\t\t%d
                            %s\t\t%d
                            %s\t%d
                            %s\t\t\t%d
                            """,
                    WORD_LIST_TABLE, wordListRows,
                    URL_LIST_TABLE, urlListRows,
                    WORD_LOCATION_TABLE, wordLocationRows,
                    LINK_BETWEEN_URL_TABLE, linkBetweenUrlRows,
                    LINK_WORD_TABLE, linkWordRows
                    );
        }
        if (dbInsertingEnable && Objects.nonNull(statisticsRepository)) {
            statisticsRepository.addRowsCount(rowsCount);
        }

        return rowsCount;
    }

    public void getTopNDomains(Integer n) {

    }

    public void getTopNWords(Integer n) {

    }

    public void getTopN(Integer n) {

    }
}
