package com.example.domain;

/** Compact input for {@link DietRecommender}, derived from the intake history. */
public record IntakeSummary(double averageDailyCookies, int daysTracked, int peakDailyCount) {}
