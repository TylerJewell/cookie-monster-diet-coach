package com.example.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic stand-in for an LLM diet coach (the prototype's "mock AI"). Produces a
 * {@link DietRecommendation} from the intake history without any model call, so it costs
 * nothing to run. The recommendation always permits a non-zero daily cookie allowance.
 */
public final class DietRecommender {

  private DietRecommender() {}

  public static DietRecommendation recommend(
      IntakeSummary summary, CookieMonsterProfile profile, LocalDate today) {

    if (summary.daysTracked() == 0) {
      return new DietRecommendation(
          6,
          List.of(
              "Start by logging every cookie so we can see " + profile.name() + "'s habits.",
              "Pair cookies with a glass of water or a piece of fruit."),
          "No history yet — here's a gentle starter plan that still keeps cookies on the menu.",
          today);
    }

    double avg = summary.averageDailyCookies();
    int allowance = Math.max(1, (int) Math.round(avg * 0.6)); // trim to ~60%, never zero

    List<String> tips = new ArrayList<>();
    tips.add("Swap one afternoon cookie for fruit to help " + profile.name() + "'s cholesterol.");
    tips.add("Take a short walk after cookie time.");

    String rationale;
    if (avg >= 6) {
      tips.add("Choose oatmeal cookies over double-chocolate when you can — a little more fiber.");
      rationale =
          String.format(
              "Intake is high (avg %.1f/day), which can raise cholesterol. We're trimming to %d"
                  + " cookies/day — still plenty of cookie joy, just healthier.",
              avg, allowance);
    } else {
      rationale =
          String.format(
              "Intake is moderate (avg %.1f/day). A target of %d cookies/day keeps %s happy while"
                  + " watching cholesterol.",
              avg, allowance, profile.name());
    }

    return new DietRecommendation(allowance, List.copyOf(tips), rationale, today);
  }
}
