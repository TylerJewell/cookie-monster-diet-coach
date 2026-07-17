package com.example.domain;

import java.time.LocalDate;
import java.util.Optional;

/** A single logged cookie-intake action. */
public record CookieEntry(String entryId, int count, Optional<String> type, LocalDate day) {}
