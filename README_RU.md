# Веб-поисковик

В рамках данного проекта были реализованы модуль индексации страниц, а также модуль обработки поисковых запросов и выдачи результатов с ранжированием. Используется среднее по 2 метрикам ранжирования: PageRank и расположение искомых слов от начала документа.

## Использованные технологии

- [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- [PostgreSQL](https://www.postgresql.org/)
- [Apache Maven](https://maven.apache.org/)
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/)
- [Jsoup](https://jsoup.org/)

## Как настроить проект на вашем компьютере

- Установить [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) и [PostgreSQL](https://www.postgresql.org/) (все необходимые таблицы создадуться при запуске автоматически)
- Клонировать [этот](https://github.com/Popov-Dmitry/web-search) репозиторий
- В файле `./src/main/resources/config.properties` изменить параметры для доступа к БД:
- - `DB.URL` - url-адрес подключения к БД
- - `DB.USERNAME` - имя пользователя
- - `DB.PASSWORD` - пароль пользователя
- - `DB.TABLE.*` - названия таблиц БД (менять не обязательно)


- Также в файле `./src/main/resources/config.properties` можно изменить параметры паука:
- - `CRAWLER.PAGES` - стартовые страницы обхода
- - `CRAWLER.DEPTH` - глубина обхода
- - `CRAWLER.FILTER` - фильтр слов, которые НЕ будут проиндексированы
- - `CRAWLER.PAGES_LIMIT` - лимит количества страниц для индексации
- - `CRAWLER.DELAY` - задержка перед индексацией очередной страницы
- - `CRAWLER.LOGGING_ENABLE` - логирование в консоль
- - `CRAWLER.DB_INSERTING_ENABLE` - логирование в БД
- - `CRAWLER.TOP_N` - количество самых популярных слов и доменов после индексации
- - `CRAWLER.STATISTICS_COLLECTION_INTERVAL_PAGES` - интервал логирования статистики
- - `CRAWLER.PAGE_RANK_ITERATIONS` - количество итераций для расчета статистики PageRank


- И параметр поисковика:
- - `SEARCHER.FILE_PATH` - путь, где сохранять размеченные HTML файлы по результатам поиска (если нужно)

## Доступное API и примеры использования

Для работы модуля паука необходимо создать объект класса CrawlerService со следующими параметрами конструктора:
- filter – массив строк, которые будут игнорироваться при индексации
- pagesLimit – лимит количества страниц для индексации
- delay – задержка перед индексацией очередной страницы
- loggingEnable – включение логирования статистики в консоль
- dbInsertingEnable – включение логирования статистики в БД
- n – лимит для вывода статистики top n
- statisticsCollectionIntervalPages – интервал логирования

Далее нужно вызвать метод crawl, куда передать начальные страницы и глубину обхода.

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

Для работы модуля поиска необходимо создать объект класса SearchService, в конструктор которого передать `путь`, где сохранять размеченые HTML файлы по результатам поиска или `null`, если сохранять не нужно.

Далее вызвать метод getSortedMap, куда передать поисковый запрос и лимит количества страниц в результате.

```java
            SearchService searchService = new SearchService(ConfigUtils.getProperty("SEARCHER.FILE_PATH"));
            searchService.getSortedMap("python json", 3);
```