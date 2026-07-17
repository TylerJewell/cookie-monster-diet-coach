package com.example.domain;

import java.time.LocalDate;

/** Aggregate cookie count for a single calendar day. */
public record DailyTotal(LocalDate day, int totalCount) {}
