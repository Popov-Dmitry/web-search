package com.github.popovdmitry.websearch.service;

import com.github.popovdmitry.websearch.repository.Repository;
import com.github.popovdmitry.websearch.repository.StatisticsRepository;
import com.github.popovdmitry.websearch.utils.ConfigUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StatisticsService {

    private final String WORD_LIST_TABLE = ConfigUtils.getProperty("WORD_LIST_TABLE");
    private final String URL_LIST_TABLE = ConfigUtils.getProperty("URL_LIST_TABLE");
    private final String WORD_LOCATION_TABLE = ConfigUtils.getProperty("WORD_LOCATION_TABLE");
    private final String LINK_BETWEEN_URL_TABLE = ConfigUtils.getProperty("LINK_BETWEEN_URL_TABLE");
    private final String LINK_WORD_TABLE = ConfigUtils.getProperty("LINK_WORD_TABLE");

    private final String TOP_N_WORDS_TABLE = ConfigUtils.getProperty("TOP_N_WORDS_TABLE");
    private final String TOP_N_DOMAINS_TABLE = ConfigUtils.getProperty("TOP_N_DOMAINS_TABLE");

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

    public void close() throws SQLException {
        if (Objects.nonNull(statisticsRepository)) {
            statisticsRepository.close();
        }

    }

    public Map<String, Map<String, Integer>> getSummary(Integer n) throws SQLException {
        Map<String, Map<String, Integer>> summary = new Hashtable<>();
        summary.put("rowsCount", getRowsCount());
        summary.put("topNWords", getTopNWords(n));
        summary.put("topNDomains", getTopNDomains(n));

        return summary;
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

    public Map<String, Integer> getTopNDomains(Integer n) throws SQLException {
        ResultSet resultSet = repository.selectAllFrom(URL_LIST_TABLE);
        Map<String, Integer> topNMap = new Hashtable<>();
        while (resultSet.next()) {
            String link = resultSet.getString(2);
            Pattern pattern = Pattern.compile("(http)(s)?(://)([A-Za-z0-9]+)((\\.)([A-Za-z0-9]+))+");
            Matcher matcher = pattern.matcher(link);
            if (matcher.find()){
                link = matcher.group();
            } else {
                continue;
            }
            topNMap.put(link, topNMap.getOrDefault(link, 0) + 1);
        }
        Map<String, Integer> topNDomainsMap = topNMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        if (loggingEnable) {
            System.out.println("\t\t Top N Domains");
            topNDomainsMap.forEach((key, value) -> System.out.println(key + ", " + value));
        }
        if (dbInsertingEnable && Objects.nonNull(statisticsRepository)) {
            statisticsRepository.addTopN(topNDomainsMap, TOP_N_DOMAINS_TABLE);
        }

        return topNDomainsMap;
    }

    public Map<String, Integer> getTopNWords(Integer n) throws SQLException {
        ResultSet resultSet = repository.selectAllFrom(WORD_LOCATION_TABLE);
        Map<Integer, Integer> topNMap = new Hashtable<>();
        while (resultSet.next()) {
            Integer wordId = resultSet.getInt(2);
            topNMap.put(wordId, topNMap.getOrDefault(wordId, 0) + 1);
        }
        Map<String, Integer> topNWordsMap = new LinkedHashMap<>();
        if (loggingEnable) {
            System.out.println("\t\t Top N Words");
        }
        topNMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .forEach((entry) -> {
                    try {
                        ResultSet resultSet1 = repository.selectFromWhere(WORD_LIST_TABLE, "row_id", entry.getKey().toString());
                        resultSet1.next();
                        String word = resultSet1.getString(2);
                        topNWordsMap.put(word, entry.getValue());
                        if (loggingEnable) {
                            System.out.println(word + ", " + entry.getValue());
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
        if (dbInsertingEnable && Objects.nonNull(statisticsRepository)) {
            statisticsRepository.addTopN(topNWordsMap, TOP_N_WORDS_TABLE);
        }

        return topNWordsMap;
    }
}
