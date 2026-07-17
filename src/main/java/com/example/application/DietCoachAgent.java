package com.example.application;

import akka.javasdk.JsonSupport;
import akka.javasdk.agent.Agent;
import akka.javasdk.agent.MemoryProvider;
import akka.javasdk.annotations.Component;
import akka.javasdk.annotations.Description;
import com.example.domain.CookieMonsterProfile;
import com.example.domain.DietRecommender;
import com.example.domain.IntakeSummary;
import java.time.LocalDate;
import java.util.List;

/**
 * Diet-coach agent for Cookie Monster. This is a real Akka {@link Agent} using the structured
 * response flow. For the prototype its model is a {@link MockDietModelProvider} — a mock LLM
 * that runs entirely offline and never uses a real model provider or API key. Swapping to a
 * live model is a one-line change (remove {@code .model(...)} to use the configured default).
 */
@Component(id = "diet-coach-agent")
public class DietCoachAgent extends Agent {

  /** Input for the coach: profile plus a compact summary of recent intake. */
  public record CoachRequest(
      String subjectName,
      int heightInches,
      String concern,
      double averageDailyCookies,
      int daysTracked,
      int peakDailyCount) {}

  /** Structured advice returned by the coach. */
  public record CoachAdvice(
      @Description("Suggested daily cookie allowance; must be greater than zero")
          int dailyCookieAllowance,
      @Description("Two or three healthier balancing tips") List<String> balancingSuggestions,
      @Description("Friendly rationale that acknowledges the cholesterol concern") String rationale) {}

  private static final String SYSTEM_MESSAGE =
      """
      You are a friendly diet coach for Cookie Monster. Keep cookies in his life — never set the
      daily cookie allowance to zero. Acknowledge his cholesterol concern and suggest a few
      healthier balancing habits. The user message is a JSON object with his profile and recent
      cookie intake; base your advice on it.
      """
          .stripIndent();

  public Effect<CoachAdvice> recommend(CoachRequest request) {
    return effects()
        .model(new MockDietModelProvider()) // prototype: mock LLM, no real provider or API key
        .memory(MemoryProvider.none())
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage(JsonSupport.encodeToString(request))
        .responseConformsTo(CoachAdvice.class)
        .onFailure(throwable -> fallback(request))
        .thenReply();
  }

  private static CoachAdvice fallback(CoachRequest request) {
    var recommendation =
        DietRecommender.recommend(
            new IntakeSummary(
                request.averageDailyCookies(), request.daysTracked(), request.peakDailyCount()),
            new CookieMonsterProfile(request.subjectName(), request.heightInches(), request.concern()),
            LocalDate.now());
    return new CoachAdvice(
        recommendation.dailyCookieAllowance(),
        recommendation.balancingSuggestions(),
        recommendation.rationale());
  }
}
