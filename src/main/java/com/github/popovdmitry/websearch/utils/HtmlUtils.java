package com.github.popovdmitry.websearch.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class HtmlUtils {

    public static void generateMarkedHtmlAndWriteFile(String queryString, List<String> words, String fileName) {
        writeFile(fileName, generateMarkedHtml(queryString, words));
    }

    public static String generateMarkedHtml(String queryString, List<String> words) {
        String[] queryWords = queryString.split("(\\s)+");
        StringBuilder result = new StringBuilder();
        result.append("""
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                  <meta charset="UTF-8">
                  <title>Search result</title>
                </head>
                <body>
                """);

        words.forEach((word) -> {
            boolean markWord = false;

            for (String queryWord : queryWords) {
                if (queryWord.equalsIgnoreCase(word)) {
                    markWord = true;
                    break;
                }
            }

            if (markWord) {
                result.append("<span style=\"background-color:#ffd700;\">");
                result.append(word);
                result.append("</span>");
            } else {
                result.append(word);
            }
            result.append(" ");
        });

        result.append("""
                </body>
                </html>
                """);

        return result.toString();
    }

    public static void writeFile(String fileName, String content) {
        try(FileWriter writer = new FileWriter(fileName, false))
        {
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
