package com.github.popovdmitry.websearch.repository;

import com.github.popovdmitry.websearch.utils.Tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CrawlerRepository extends Repository {

    public CrawlerRepository() throws SQLException {
        super();
        init();
    }

    private void init() throws SQLException {
        Statement statement = getConnection().createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                "(row_id serial constraint wordlist_pk primary key, word text, isFiltered int);",
                Tables.WORD_LIST_TABLE));
        statement.close();

        statement = getConnection().createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                "(row_id serial constraint urllist_pk primary key, url text);",
                Tables.URL_LIST_TABLE));
        statement.close();

        statement = getConnection().createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                "(row_id serial constraint wordlocation_pk primary key, " +
                "word_id integer constraint wordlocation_wordlist_rowid_fk references word_list, " +
                "url_id integer constraint wordlocation_urllist_rowid_fk references url_list," +
                "location integer);",
                Tables.WORD_LOCATION_TABLE));
        statement.close();

        statement = getConnection().createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                "(row_id serial not null constraint linkbetweenurl_pk primary key, " +
                "from_url_id integer constraint linkbetweenurl_urllist_rowid_fk references url_list, " +
                "to_url_id integer constraint linkbetweenurl_urllist_rowid_fk_2 references url_list);",
                Tables.LINK_BETWEEN_URL_TABLE));
        statement.close();

        statement = getConnection().createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                "(row_id serial not null constraint linkword_pk primary key, " +
                "word_id integer constraint linkword_wordlist_rowid_fk references word_list," +
                "link_id integer constraint linkword_linkbetweenurl_rowid_fk references link_between_url);",
                Tables.LINK_WORD_TABLE));
        statement.close();
    }

    public Integer addWord(String word, Integer isFiltered) throws SQLException {
        ResultSet resultSet = selectFromWhere(Tables.WORD_LIST_TABLE, "word", word);
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }
        
        PreparedStatement preparedStatement =
                getConnection().prepareStatement(String.format(
                        "INSERT INTO %s (word, is_filtered) VALUES (?, ?);",
                        Tables.WORD_LIST_TABLE), Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setString(1, word);
        preparedStatement.setInt(2, isFiltered);
        preparedStatement.executeUpdate();
        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
        generatedKeys.next();
        Integer wordId = generatedKeys.getInt(1);
        preparedStatement.close();
        return wordId;
    }

    public void addWordLocation(Integer wordId, Integer urlId, Integer location) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(String.format(
                "SELECT * FROM %s WHERE (word_id = ? AND url_id = ? AND location = ?);",
                Tables.WORD_LOCATION_TABLE
        ));
        preparedStatement.setInt(1, wordId);
        preparedStatement.setInt(2, urlId);
        preparedStatement.setInt(3, location);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            return;
        }

        preparedStatement =
                getConnection().prepareStatement(String.format(
                        "INSERT INTO %s (word_id, url_id, location) VALUES (?, ?, ?);",
                        Tables.WORD_LOCATION_TABLE));
        preparedStatement.setInt(1, wordId);
        preparedStatement.setInt(2, urlId);
        preparedStatement.setInt(3, location);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public Integer addUrl(String url) throws SQLException {
        ResultSet resultSet = selectFromWhere(Tables.URL_LIST_TABLE, "url", url);
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        PreparedStatement preparedStatement =
                getConnection().prepareStatement(String.format(
                        "INSERT INTO %s (url) VALUES (?);",
                        Tables.URL_LIST_TABLE), Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setString(1, url);
        preparedStatement.executeUpdate();
        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
        generatedKeys.next();
        Integer urlId = generatedKeys.getInt(1);
        preparedStatement.close();
        return urlId;
    }

    public Integer addLinkBetweenUrl(Integer fromUrlId, Integer toUrlId) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(String.format(
                "SELECT * FROM %s WHERE (from_url_id = ? AND to_url_id = ?);",
                Tables.LINK_BETWEEN_URL_TABLE
        ));
        preparedStatement.setInt(1, fromUrlId);
        preparedStatement.setInt(2, toUrlId);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        preparedStatement =
                getConnection().prepareStatement(String.format(
                        "INSERT INTO %s (from_url_id, to_url_id) VALUES (?, ?);",
                        Tables.LINK_BETWEEN_URL_TABLE), Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setInt(1, fromUrlId);
        preparedStatement.setInt(2, toUrlId);
        preparedStatement.executeUpdate();
        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
        generatedKeys.next();
        Integer linkBetweenUrlId = generatedKeys.getInt(1);
        preparedStatement.close();
        return linkBetweenUrlId;
    }

    public void addLinkText(String[] linkWords, Integer linkBetweenUrlId) throws SQLException {
        PreparedStatement preparedStatement =
                getConnection().prepareStatement(String.format(
                        "INSERT INTO %s (word_id, link_id) VALUES (?, ?);",
                        Tables.LINK_WORD_TABLE));
        getConnection().setAutoCommit(false);

        for (String word : linkWords) {
            ResultSet resultSet = selectFromWhere(Tables.WORD_LIST_TABLE, "word", word);
            resultSet.next();
            int wordId = resultSet.getInt(1);
            preparedStatement.setInt(1, wordId);
            preparedStatement.setInt(2, linkBetweenUrlId);
            preparedStatement.addBatch();

        }
        getConnection().commit();
        getConnection().setAutoCommit(true);

        preparedStatement.executeBatch();
        preparedStatement.close();
    }

    public void preparePageRankTable() throws SQLException {
        Statement statement = getConnection().createStatement();
        statement.execute(String.format("DROP TABLE IF EXISTS %s", Tables.PAGE_RANK_TABLE));
        statement.execute(String.format(
                "CREATE TABLE  IF NOT EXISTS %s (row_id SERIAL, url_id INTEGER, score REAL);",
                Tables.PAGE_RANK_TABLE
        ));

        statement.execute("DROP INDEX IF EXISTS word_idx;");
        statement.execute("DROP INDEX IF EXISTS url_idx;");
        statement.execute("DROP INDEX IF EXISTS word_url_idx;");
        statement.execute("DROP INDEX IF EXISTS url_to_idx;");
        statement.execute("DROP INDEX IF EXISTS url_from_idx;");

        statement.execute(String.format("CREATE INDEX IF NOT EXISTS word_idx ON %s(word);", Tables.WORD_LIST_TABLE));
        statement.execute(String.format("CREATE INDEX IF NOT EXISTS url_idx ON %s(url);", Tables.URL_LIST_TABLE));
        statement.execute(String.format("CREATE INDEX IF NOT EXISTS word_url_idx ON %s(word_id);", Tables.WORD_LOCATION_TABLE));
        statement.execute(String.format("CREATE INDEX IF NOT EXISTS url_to_idx ON %s(to_url_id);", Tables.LINK_BETWEEN_URL_TABLE));
        statement.execute(String.format("CREATE INDEX IF NOT EXISTS url_from_idx ON %s(from_url_id);", Tables.LINK_BETWEEN_URL_TABLE));

        statement.execute("DROP INDEX IF EXISTS rank_url_id_idx;");
        statement.execute(String.format("CREATE INDEX IF NOT EXISTS rank_url_id_idx ON %s(url_id)", Tables.PAGE_RANK_TABLE));

        statement.execute("REINDEX word_idx;");
        statement.execute("REINDEX url_idx;");
        statement.execute("REINDEX word_url_idx;");
        statement.execute("REINDEX url_to_idx;");
        statement.execute("REINDEX url_from_idx;");
        statement.execute("REINDEX rank_url_id_idx;");

        statement.execute(String.format(
                "INSERT INTO %s (url_id, score) SELECT row_id, 1.0 FROM %s;",
                Tables.PAGE_RANK_TABLE,
                Tables.URL_LIST_TABLE
        ));
        getConnection().commit();
    }

    public ResultSet getDistinctFromUlrId(Integer toUrlId) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(String.format(
                "SELECT DISTINCT from_id FROM %s WHERE to_url_id = ?;",
                Tables.LINK_BETWEEN_URL_TABLE
        ));
        preparedStatement.setInt(1, toUrlId);

        return preparedStatement.executeQuery();
    }
}
