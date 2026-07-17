package com.example.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * State of the cookie-intake log for the single tracked subject: the profile, all logged
 * entries, and the most recent diet recommendation. Holds the feature's business logic and
 * is free of any Akka framework types.
 */
public record CookieLog(
    CookieMonsterProfile profile,
    List<CookieEntry> entries,
    Optional<DietRecommendation> latestRecommendation) {

  public static CookieLog empty() {
    return new CookieLog(CookieMonsterProfile.defaultProfile(), List.of(), Optional.empty());
  }

  public CookieLog addEntry(CookieEntry entry) {
    var updated = new ArrayList<>(entries);
    updated.add(entry);
    return new CookieLog(profile, List.copyOf(updated), latestRecommendation);
  }

  public CookieLog removeEntry(String entryId) {
    var updated = entries.stream().filter(e -> !e.entryId().equals(entryId)).toList();
    return new CookieLog(profile, updated, latestRecommendation);
  }

  public boolean hasEntry(String entryId) {
    return entries.stream().anyMatch(e -> e.entryId().equals(entryId));
  }

  public List<CookieEntry> entriesForDay(LocalDate day) {
    return entries.stream().filter(e -> e.day().equals(day)).toList();
  }

  public int totalForDay(LocalDate day) {
    return entries.stream().filter(e -> e.day().equals(day)).mapToInt(CookieEntry::count).sum();
  }

  /** Per-day totals for the most recent {@code n} days that have entries, newest first. */
  public List<DailyTotal> recentDays(int n) {
    var byDay = new TreeMap<LocalDate, Integer>(Comparator.reverseOrder());
    for (var e : entries) {
      byDay.merge(e.day(), e.count(), Integer::sum);
    }
    return byDay.entrySet().stream()
        .limit(n)
        .map(en -> new DailyTotal(en.getKey(), en.getValue()))
        .toList();
  }

  public IntakeSummary summarize() {
    if (entries.isEmpty()) {
      return new IntakeSummary(0.0, 0, 0);
    }
    var byDay = new HashMap<LocalDate, Integer>();
    for (var e : entries) {
      byDay.merge(e.day(), e.count(), Integer::sum);
    }
    int days = byDay.size();
    int total = byDay.values().stream().mapToInt(Integer::intValue).sum();
    int peak = byDay.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    return new IntakeSummary((double) total / days, days, peak);
  }

  public CookieLog withRecommendation(DietRecommendation recommendation) {
    return new CookieLog(profile, entries, Optional.of(recommendation));
  }
}
