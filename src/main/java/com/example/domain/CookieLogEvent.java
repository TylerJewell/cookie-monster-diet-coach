package com.example.domain;

import akka.javasdk.annotations.TypeName;
import java.time.LocalDate;
import java.util.Optional;

/** Events emitted by the cookie-intake log. */
public sealed interface CookieLogEvent {

  @TypeName("cookie-logged")
  record CookieLogged(String entryId, int count, Optional<String> type, LocalDate day)
      implements CookieLogEvent {}

  @TypeName("entry-removed")
  record EntryRemoved(String entryId) implements CookieLogEvent {}

  @TypeName("recommendation-generated")
  record RecommendationGenerated(DietRecommendation recommendation) implements CookieLogEvent {}
}
