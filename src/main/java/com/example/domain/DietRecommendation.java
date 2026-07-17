package com.example.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * The diet-coach output. In the prototype this is produced deterministically by
 * {@link DietRecommender}; the shape is kept LLM-ready for a future agent swap.
 * {@code dailyCookieAllowance} is always greater than zero — cookies are never eliminated.
 */
public record DietRecommendation(
    int dailyCookieAllowance,
    List<String> balancingSuggestions,
    String rationale,
    LocalDate generatedOn) {}
