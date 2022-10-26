package com.github.popovdmitry.websearch.record;

import java.util.List;

public record MatchWordsRecord(List<Integer> wordsIds, List<List<Integer>> locationCombinations) { }
