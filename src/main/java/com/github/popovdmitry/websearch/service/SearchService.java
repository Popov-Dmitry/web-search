package com.github.popovdmitry.websearch.service;

import com.github.popovdmitry.websearch.exception.NotFoundException;
import com.github.popovdmitry.websearch.record.MatchWordsRecord;
import com.github.popovdmitry.websearch.repository.SearchRepository;
import com.github.popovdmitry.websearch.utils.Tables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SearchService {

    private final SearchRepository searchRepository;

    public SearchService() throws SQLException {
        this.searchRepository = new SearchRepository();
    }

    public List<Integer> getWordsIds(String queryString) throws SQLException, NotFoundException {
        String[] words = queryString.trim().split("(\\s)+");
        List<Integer> wordIdsList = new ArrayList<>();
        for (String word : words) {
            ResultSet resultSet = searchRepository.selectFromWhere(Tables.WORD_LIST_TABLE, "word", word);
            if (resultSet.next()) {
                wordIdsList.add(resultSet.getInt(1));
            }
        }

        if (wordIdsList.isEmpty()) {
            throw new NotFoundException(String.format("No results found for \"%s\"", queryString));
        }

        return wordIdsList;
    }

    public MatchWordsRecord getMatchWords(String queryString) throws SQLException, NotFoundException {
        String[] words = queryString.trim().split("(\\s)+");
        List<Integer> wordsIds = getWordsIds(queryString);
        List<List<Integer>> locationCombinations = searchRepository.selectMatchWords(words, wordsIds);
        return new MatchWordsRecord(wordsIds, locationCombinations);
    }

    public Map<Integer, Double> getLocationScore(List<List<Integer>> locationCombinations) {
        Map<Integer, Integer> locationMap = locationCombinations.stream()
                .collect(Collectors.toMap(
                        (row) -> row.get(0),
                        (row) -> 1000000,
                        (value1, value2) -> value1
                ));
        locationCombinations.forEach((row) -> {
            int sum = 0;
            for (int i = 1; i < row.size(); i++) {
                sum += row.get(i);
            }
            if (sum < locationMap.get(row.get(0))) {
                locationMap.put(row.get(0), sum);
            }
        });

        return normalizeScores(locationMap, true);
    }

    public Map<Integer, Double> normalizeScores(Map<Integer, Integer> scores, Boolean smallIsBetter) {
        Map<Integer, Double> result = new Hashtable<>();
        double smallValue = 0.00001;
        Integer minScore = Collections.min(scores.values());
        Integer maxScore = Collections.max(scores.values());

        scores.forEach((key, value) -> {
            if (smallIsBetter) {
                result.put(key, minScore / Math.max(smallValue, value));
            } else {
                result.put(key, (double) (value / maxScore));
            }
        });

        return result;
    }

    public Map<String, Double> getSortedMap(String queryString, Integer limit) throws SQLException, NotFoundException {
        if (limit < 1) {
            limit = Integer.MAX_VALUE;
        }
        MatchWordsRecord matchWords = getMatchWords(queryString);
        Map<Integer, Double> locationScores = getLocationScore(matchWords.locationCombinations());

        return locationScores.entrySet()
                .stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        (entry) -> {
                            try {
                                ResultSet resultSet = searchRepository.selectFromWhere(
                                        Tables.URL_LIST_TABLE,
                                        "row_id",
                                        entry.getKey()
                                );
                                resultSet.next();

                                return resultSet.getString(2);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            return entry.getKey().toString();
                        },
                        Map.Entry::getValue,
                        (value1, value2) -> value1,
                        LinkedHashMap::new
                ));
    }
}
