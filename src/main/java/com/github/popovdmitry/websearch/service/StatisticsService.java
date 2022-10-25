package com.github.popovdmitry.websearch.service;

import com.github.popovdmitry.websearch.repository.CrawlerRepository;
import com.github.popovdmitry.websearch.repository.StatisticsRepository;
import com.github.popovdmitry.websearch.utils.Tables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StatisticsService {

    private final Boolean loggingEnable;
    private final Boolean dbInsertingEnable;
    private final CrawlerRepository crawlerRepository;
    private final StatisticsRepository statisticsRepository;

    public StatisticsService(Boolean loggingEnable, Boolean dbInsertingEnable, CrawlerRepository crawlerRepository) throws SQLException {
        this.loggingEnable = loggingEnable;
        this.dbInsertingEnable = dbInsertingEnable;
        this.crawlerRepository = crawlerRepository;
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
        Integer wordListRows = crawlerRepository.selectRowsCountFrom(Tables.WORD_LIST_TABLE);
        Integer urlListRows = crawlerRepository.selectRowsCountFrom(Tables.URL_LIST_TABLE);
        Integer wordLocationRows = crawlerRepository.selectRowsCountFrom(Tables.WORD_LOCATION_TABLE);
        Integer linkBetweenUrlRows = crawlerRepository.selectRowsCountFrom(Tables.LINK_BETWEEN_URL_TABLE);
        Integer linkWordRows = crawlerRepository.selectRowsCountFrom(Tables.LINK_WORD_TABLE);
        rowsCount.put(Tables.WORD_LIST_TABLE, wordListRows);
        rowsCount.put(Tables.URL_LIST_TABLE, urlListRows);
        rowsCount.put(Tables.WORD_LOCATION_TABLE, wordLocationRows);
        rowsCount.put(Tables.LINK_BETWEEN_URL_TABLE, linkBetweenUrlRows);
        rowsCount.put(Tables.LINK_WORD_TABLE, linkWordRows);

        if (loggingEnable) {
            System.out.printf("""
                            		 Rows Count
                            %s\t\t\t%d
                            %s\t\t\t%d
                            %s\t\t%d
                            %s\t%d
                            %s\t\t\t%d
                            """,
                    Tables.WORD_LIST_TABLE, wordListRows,
                    Tables.URL_LIST_TABLE, urlListRows,
                    Tables.WORD_LOCATION_TABLE, wordLocationRows,
                    Tables.LINK_BETWEEN_URL_TABLE, linkBetweenUrlRows,
                    Tables.LINK_WORD_TABLE, linkWordRows
                    );
        }
        if (dbInsertingEnable && Objects.nonNull(statisticsRepository)) {
            statisticsRepository.addRowsCount(rowsCount);
        }

        return rowsCount;
    }

    public Map<String, Integer> getTopNDomains(Integer n) throws SQLException {
        ResultSet resultSet = crawlerRepository.selectAllFrom(Tables.URL_LIST_TABLE);
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
            statisticsRepository.addTopN(topNDomainsMap, Tables.TOP_N_DOMAINS_TABLE);
        }

        return topNDomainsMap;
    }

    public Map<String, Integer> getTopNWords(Integer n) throws SQLException {
        ResultSet resultSet = crawlerRepository.selectAllFrom(Tables.WORD_LOCATION_TABLE);
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
                        ResultSet resultSet1 = crawlerRepository.selectFromWhere(
                                Tables.WORD_LIST_TABLE,
                                "row_id",
                                entry.getKey()
                        );
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
            statisticsRepository.addTopN(topNWordsMap, Tables.TOP_N_WORDS_TABLE);
        }

        return topNWordsMap;
    }

    public void collectWordsCount(Integer pagesProcessedCount) throws SQLException {
        Integer wordsCount = crawlerRepository.selectRowsCountFrom(Tables.WORD_LIST_TABLE);
        if (loggingEnable) {
            System.out.printf("Processed %d pages and %d words%n", pagesProcessedCount, wordsCount);
        }
        if (dbInsertingEnable && Objects.nonNull(statisticsRepository)) {
            statisticsRepository.addRowsCount(pagesProcessedCount, wordsCount, Tables.WORDS_COUNT_TABLE);
        }
    }

    public void collectLinkBetweenUrlCount(Integer pagesProcessedCount) throws SQLException {
        Integer linksBetweenUrlCount = crawlerRepository.selectRowsCountFrom(Tables.LINK_BETWEEN_URL_TABLE);
        if (loggingEnable) {
            System.out.printf("Processed %d pages and %d links between url%n", pagesProcessedCount, linksBetweenUrlCount);
        }
        if (dbInsertingEnable && Objects.nonNull(statisticsRepository)) {
            statisticsRepository.addRowsCount(pagesProcessedCount, linksBetweenUrlCount, Tables.LINK_BETWEEN_URL_COUNT_TABLE);
        }
    }

    public void collectRowsCountStatistics(Integer pagesProcessedCount) throws SQLException {
        collectWordsCount(pagesProcessedCount);
        collectLinkBetweenUrlCount(pagesProcessedCount);
    }
}
