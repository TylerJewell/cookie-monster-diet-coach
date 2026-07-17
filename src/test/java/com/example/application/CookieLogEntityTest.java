package com.example.application;

import static org.assertj.core.api.Assertions.assertThat;

import akka.javasdk.testkit.EventSourcedTestKit;
import com.example.domain.CookieLog;
import com.example.domain.CookieLogEvent;
import com.example.domain.DietRecommendation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class CookieLogEntityTest {

  private EventSourcedTestKit<CookieLog, CookieLogEvent, CookieLogEntity> testKit() {
    return EventSourcedTestKit.of("cookie-monster", CookieLogEntity::new);
  }

  @Test
  public void logsCookiesAndTracksDailyTotal() {
    var testKit = testKit();

    var result =
        testKit
            .method(CookieLogEntity::logCookies)
            .invoke(new CookieLogEntity.LogCookies(3, Optional.of("chocolate chip")));

    assertThat(result.isReply()).isTrue();
    assertThat(testKit.getState().totalForDay(LocalDate.now())).isEqualTo(3);
  }

  @Test
  public void rejectsNonPositiveCount() {
    var testKit = testKit();

    var result =
        testKit
            .method(CookieLogEntity::logCookies)
            .invoke(new CookieLogEntity.LogCookies(0, Optional.empty()));

    assertThat(result.isError()).isTrue();
  }

  @Test
  public void removeEntryUpdatesTotal() {
    var testKit = testKit();
    testKit.method(CookieLogEntity::logCookies).invoke(new CookieLogEntity.LogCookies(5, Optional.empty()));
    var entryId = testKit.getState().entries().get(0).entryId();

    var result = testKit.method(CookieLogEntity::removeEntry).invoke(entryId);

    assertThat(result.isReply()).isTrue();
    assertThat(testKit.getState().totalForDay(LocalDate.now())).isZero();
  }

  @Test
  public void removeUnknownEntryFails() {
    var testKit = testKit();

    var result = testKit.method(CookieLogEntity::removeEntry).invoke("does-not-exist");

    assertThat(result.isError()).isTrue();
  }

  @Test
  public void recordsRecommendation() {
    var testKit = testKit();
    var recommendation =
        new DietRecommendation(5, List.of("Swap a cookie for fruit."), "Watch that cholesterol.", LocalDate.now());

    var result = testKit.method(CookieLogEntity::recordRecommendation).invoke(recommendation);

    assertThat(result.isReply()).isTrue();
    assertThat(testKit.getState().latestRecommendation()).contains(recommendation);
  }
}
