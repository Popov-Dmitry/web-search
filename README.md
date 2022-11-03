# Web-search

In this project have implemented a page indexing module, as well as a module for processing search queries and issuing results with ranking. The average of 2 ranking metrics is used: PageRank and the location of the search words from the beginning of the document.

## Used technologies

- [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- [PostgreSQL](https://www.postgresql.org/)
- [Apache Maven](https://maven.apache.org/)
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/)
- [Jsoup](https://jsoup.org/)

## How to set up a project on your computer

- Install [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) and [PostgreSQL](https://www.postgresql.org/) (all necessary tables will be created automatically at startup)
- Clone [this](https://github.com/Popov-Dmitry/web-search) repository
- In the file `./src/main/resources/config.properties` change DB access variables:
- - `DB.URL`
- - `DB.USERNAME`
- - `DB.PASSWORD`
- - `DB.TABLE.*` - database tables names (not required to be changed)


- Also in the file `./src/main/resources/config.properties` you can change the parameters of the crawler:
- - `CRAWLER.PAGES` - crawl start pages
- - `CRAWLER.DEPTH` - crawling depth
- - `CRAWLER.FILTER` - filter words that will NOT be indexed
- - `CRAWLER.PAGES_LIMIT` - pages limit for indexing
- - `CRAWLER.DELAY` - delay before indexing the next page
- - `CRAWLER.LOGGING_ENABLE` - logging to console
- - `CRAWLER.DB_INSERTING_ENABLE` - logging to DB
- - `CRAWLER.TOP_N` - number of most popular words and domains after indexing
- - `CRAWLER.STATISTICS_COLLECTION_INTERVAL_PAGES` - statistics logging interval
- - `CRAWLER.PAGE_RANK_ITERATIONS` - number of iterations to calculate PageRank statistics


- And searcher parameters:
- - `SEARCHER.FILE_PATH` - path where to save markup HTML files based on search results (if needed)

## Available API and usage examples

To make the crawler module work, you need to create an object of the CrawlerService class with the following constructor parameters:
- filter - an array of strings that will be ignored when indexing
- pagesLimit - limit of the number of pages for indexing
- delay - delay before indexing the next page
- loggingEnable - enable logging statistics to the console
- dbInsertingEnable - enable statistics logging in the DB
- n - limit for displaying top n statistics
- statisticsCollectionIntervalPages - logging interval

Next, you need to call the crawl method, where to pass the initial pages and crawl depth.

```java
CrawlerService crawlerService = new CrawlerService(
                    ConfigUtils.getProperty("CRAWLER.FILTER").split(","),
                    ConfigUtils.getIntegerProperty("CRAWLER.PAGES_LIMIT"),
                    ConfigUtils.getIntegerProperty("CRAWLER.DELAY"),
                    ConfigUtils.getBooleanProperty("CRAWLER.LOGGING_ENABLE"),
                    ConfigUtils.getBooleanProperty("CRAWLER.DB_INSERTING_ENABLE"),
                    ConfigUtils.getIntegerProperty("CRAWLER.TOP_N"),
                    ConfigUtils.getIntegerProperty("CRAWLER.STATISTICS_COLLECTION_INTERVAL_PAGES")
            );
            crawlerService.crawl(
                    List.of(ConfigUtils.getProperty("CRAWLER.PAGES").split(",")),
                    Integer.valueOf(ConfigUtils.getProperty("CRAWLER.DEPTH"))
            );
```

To make the search module work, you need to create an object of the SearchService class, to the constructor of which pass the `path` where to save the markup HTML files according to the search results or `null` if it's not necessary to save.

Next, call the getSortedMap method, where to pass the search query and the limit on the number of pages as a result.
```java
            SearchService searchService = new SearchService(ConfigUtils.getProperty("SEARCHER.FILE_PATH"));
            searchService.getSortedMap("python json", 3);
```