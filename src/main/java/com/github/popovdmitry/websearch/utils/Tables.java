package com.github.popovdmitry.websearch.utils;

public class Tables {
    public static final String WORD_LIST_TABLE = ConfigUtils.getProperty("DB.TABLE.WORD_LIST");
    public static final String URL_LIST_TABLE = ConfigUtils.getProperty("DB.TABLE.URL_LIST");
    public static final String WORD_LOCATION_TABLE = ConfigUtils.getProperty("DB.TABLE.WORD_LOCATION");
    public static final String LINK_BETWEEN_URL_TABLE = ConfigUtils.getProperty("DB.TABLE.LINK_BETWEEN_URL");
    public static final String LINK_WORD_TABLE = ConfigUtils.getProperty("DB.TABLE.LINK_WORD");

    public static final String WORDS_COUNT_TABLE = ConfigUtils.getProperty("DB.TABLE.WORDS_COUNT");
    public static final String LINK_BETWEEN_URL_COUNT_TABLE = ConfigUtils.getProperty("DB.TABLE.LINK_BETWEEN_URL_COUNT");
    public static final String ROWS_COUNT_TABLE = ConfigUtils.getProperty("DB.TABLE.ROWS_COUNT");
    public static final String TOP_N_WORDS_TABLE = ConfigUtils.getProperty("DB.TABLE.TOP_N_WORDS");
    public static final String TOP_N_DOMAINS_TABLE = ConfigUtils.getProperty("DB.TABLE.TOP_N_DOMAINS");
}
