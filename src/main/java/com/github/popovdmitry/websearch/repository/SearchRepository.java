package com.github.popovdmitry.websearch.repository;

import com.github.popovdmitry.websearch.utils.Tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SearchRepository extends Repository {

    public SearchRepository() throws SQLException {
        super();
    }

    @Override
    public ResultSet selectFromWhere(String table, String name, Integer value) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(String.format(
                "SELECT * FROM %s WHERE %s = ? LIMIT 1;",
                table,
                name
        ));
        preparedStatement.setInt(1, value);
        return preparedStatement.executeQuery();
    }

    public List<List<Integer>> selectMatchWords(String[] words, List<Integer> wordsIds) throws SQLException {
        StringBuilder sqlFullQuery = new StringBuilder();
        List<String> sqlPartName = new ArrayList<>();
        List<String> sqlPartJoin = new ArrayList<>();
        List<String> sqlPartCondition = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            Integer wordId = wordsIds.get(i);
            if (i == 0) {
                sqlPartName.add("w0.url_id url_id");
                sqlPartName.add(", w0.location w0_loc");
                sqlPartCondition.add(String.format("WHERE w0.word_id = %d", wordId));
            } else {
                sqlPartName.add(String.format(" , w%d.location w%d_loc", i, i));
                sqlPartJoin.add(String.format("INNER JOIN word_location w%d on w0.url_id=w%d.url_id", i, i));
                sqlPartCondition.add(String.format(" AND w%d.word_id=%d", i, wordId));
            }
        }

        sqlFullQuery.append("SELECT ");
        for (String name : sqlPartName) {
            sqlFullQuery.append("\n");
            sqlFullQuery.append(name);
        }

        sqlFullQuery.append("\n");
        sqlFullQuery.append("FROM word_location w0");

        for (String join : sqlPartJoin) {
            sqlFullQuery.append("\n");
            sqlFullQuery.append(join);
        }

        for (String condition : sqlPartCondition) {
            sqlFullQuery.append("\n");
            sqlFullQuery.append(condition);
        }

        ResultSet resultSet = getConnection().prepareStatement(sqlFullQuery.toString()).executeQuery();
        int columnCount = resultSet.getMetaData().getColumnCount();
        List<List<Integer>> result = new ArrayList<>();

        while (resultSet.next()) {
            List<Integer> row = new ArrayList<>();
            for (int i = 0; i < columnCount; i++) {
                row.add(resultSet.getInt(i + 1));
            }
            result.add(row);
        }

        return result;
    }

    public List<String> selectTextByUrlId(Integer urlId) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(String.format(
                "SELECT word FROM %s w_loc JOIN %s wl on w_loc.word_id = wl.row_id",
                Tables.WORD_LOCATION_TABLE,
                Tables.WORD_LIST_TABLE
        ));
        ResultSet resultSet = preparedStatement.executeQuery();
        List<String> words = new ArrayList<>();

        while (resultSet.next()) {
            words.add(resultSet.getString(1));
        }

        return words;
    }
}
