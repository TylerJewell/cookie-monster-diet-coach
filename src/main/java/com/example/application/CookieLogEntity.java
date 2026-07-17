package com.example.application;

import akka.Done;
import akka.javasdk.annotations.Component;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import com.example.domain.CookieEntry;
import com.example.domain.CookieLog;
import com.example.domain.CookieLogEvent;
import com.example.domain.DietRecommendation;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Event Sourced Entity tracking Cookie Monster's cookie intake and his latest diet
 * recommendation. There is a single instance (id {@code "cookie-monster"}).
 */
@Component(id = "cookie-log")
public class CookieLogEntity extends EventSourcedEntity<CookieLog, CookieLogEvent> {

  /** Command to log an intake entry. {@code type} is optional. */
  public record LogCookies(int count, Optional<String> type) {}

  public Effect<Done> logCookies(LogCookies command) {
    if (command.count() <= 0) {
      return effects().error("Cookie count must be greater than zero");
    }
    var event =
        new CookieLogEvent.CookieLogged(
            UUID.randomUUID().toString(), command.count(), command.type(), LocalDate.now());
    return effects().persist(event).thenReply(newState -> Done.getInstance());
  }

  public Effect<Done> removeEntry(String entryId) {
    if (!currentState().hasEntry(entryId)) {
      return effects().error("No such entry: " + entryId);
    }
    return effects()
        .persist(new CookieLogEvent.EntryRemoved(entryId))
        .thenReply(newState -> Done.getInstance());
  }

  public Effect<Done> recordRecommendation(DietRecommendation recommendation) {
    return effects()
        .persist(new CookieLogEvent.RecommendationGenerated(recommendation))
        .thenReply(newState -> Done.getInstance());
  }

  public Effect<CookieLog> getState() {
    return effects().reply(currentState());
  }

  @Override
  public CookieLog emptyState() {
    return CookieLog.empty();
  }

  @Override
  public CookieLog applyEvent(CookieLogEvent event) {
    return switch (event) {
      case CookieLogEvent.CookieLogged e ->
          currentState().addEntry(new CookieEntry(e.entryId(), e.count(), e.type(), e.day()));
      case CookieLogEvent.EntryRemoved e -> currentState().removeEntry(e.entryId());
      case CookieLogEvent.RecommendationGenerated e ->
          currentState().withRecommendation(e.recommendation());
    };
  }
}
