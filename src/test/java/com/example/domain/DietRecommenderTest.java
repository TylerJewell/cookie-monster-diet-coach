package com.example.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class DietRecommenderTest {

  private final CookieMonsterProfile profile = CookieMonsterProfile.defaultProfile();
  private final LocalDate today = LocalDate.of(2026, 7, 16);

  @Test
  public void highIntakeStillAllowsCookiesAndAddressesCholesterol() {
    var rec = DietRecommender.recommend(new IntakeSummary(20.0, 5, 25), profile, today);

    assertThat(rec.dailyCookieAllowance()).isGreaterThan(0);
    assertThat(rec.rationale().toLowerCase()).contains("cholesterol");
    assertThat(rec.balancingSuggestions()).isNotEmpty();
  }

  @Test
  public void moderateIntakeGivesPositiveAllowance() {
    var rec = DietRecommender.recommend(new IntakeSummary(3.0, 4, 4), profile, today);

    assertThat(rec.dailyCookieAllowance()).isGreaterThan(0);
    assertThat(rec.balancingSuggestions()).isNotEmpty();
  }

  @Test
  public void noHistoryReturnsGentleStarterPlan() {
    var rec = DietRecommender.recommend(new IntakeSummary(0.0, 0, 0), profile, today);

    assertThat(rec.dailyCookieAllowance()).isGreaterThan(0);
    assertThat(rec.balancingSuggestions()).isNotEmpty();
    assertThat(rec.generatedOn()).isEqualTo(today);
  }
}
