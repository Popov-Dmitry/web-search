package com.github.popovdmitry.websearch.service;

import com.github.popovdmitry.websearch.exception.NotFoundException;
import com.github.popovdmitry.websearch.record.MatchWordsRecord;
import com.github.popovdmitry.websearch.repository.SearchRepository;
import com.github.popovdmitry.websearch.utils.Tables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
}
