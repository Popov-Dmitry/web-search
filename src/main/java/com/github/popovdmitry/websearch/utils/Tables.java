package com.github.popovdmitry.websearch.utils;

public class Tables {
    public static final String WORD_LIST_TABLE = ConfigUtils.getProperty("WORD_LIST_TABLE");
    public static final String URL_LIST_TABLE = ConfigUtils.getProperty("URL_LIST_TABLE");
    public static final String WORD_LOCATION_TABLE = ConfigUtils.getProperty("WORD_LOCATION_TABLE");
    public static final String LINK_BETWEEN_URL_TABLE = ConfigUtils.getProperty("LINK_BETWEEN_URL_TABLE");
    public static final String LINK_WORD_TABLE = ConfigUtils.getProperty("LINK_WORD_TABLE");
    public static final String WORDS_COUNT_TABLE = ConfigUtils.getProperty("WORDS_COUNT_TABLE");
    public static final String LINK_BETWEEN_URL_COUNT_TABLE = ConfigUtils.getProperty("LINK_BETWEEN_URL_COUNT_TABLE");
    public static final String ROWS_COUNT_TABLE = ConfigUtils.getProperty("ROWS_COUNT_TABLE");
    public static final String TOP_N_WORDS_TABLE = ConfigUtils.getProperty("TOP_N_WORDS_TABLE");
    public static final String TOP_N_DOMAINS_TABLE = ConfigUtils.getProperty("TOP_N_DOMAINS_TABLE");
}
