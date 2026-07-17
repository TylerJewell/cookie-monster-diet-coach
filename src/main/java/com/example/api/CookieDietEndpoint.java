package com.example.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Delete;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import com.example.application.CookieLogEntity;
import com.example.application.DietCoachAgent;
import com.example.domain.CookieEntry;
import com.example.domain.CookieLog;
import com.example.domain.DietRecommendation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** HTTP API for tracking Cookie Monster's cookie intake and diet recommendations. */
@HttpEndpoint("/api")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class CookieDietEndpoint {

  private static final String SUBJECT = "cookie-monster";

  private final ComponentClient componentClient;

  public CookieDietEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  // --- API types (isolated from domain) ---

  public record LogCookieRequest(int count, String type) {}

  public record EntryView(String entryId, int count, String type, String day) {}

  public record DailyTotalView(String day, int total) {}

  public record RecommendationView(
      int dailyCookieAllowance, List<String> balancingSuggestions, String rationale, String generatedOn) {}

  public record SummaryView(
      String day,
      int todayTotal,
      List<EntryView> todayEntries,
      List<DailyTotalView> recentDays,
      RecommendationView latestRecommendation) {}

  // --- Routes ---

  @Post("/cookies")
  public HttpResponse logCookies(LogCookieRequest request) {
    if (request.count() <= 0) {
      return HttpResponses.badRequest("Cookie count must be greater than zero");
    }
    var type = Optional.ofNullable(request.type()).filter(t -> !t.isBlank());
    componentClient
        .forEventSourcedEntity(SUBJECT)
        .method(CookieLogEntity::logCookies)
        .invoke(new CookieLogEntity.LogCookies(request.count(), type));
    return HttpResponses.created(currentSummary(), "/api/summary");
  }

  @Delete("/cookies/{entryId}")
  public HttpResponse removeEntry(String entryId) {
    if (!currentState().hasEntry(entryId)) {
      return HttpResponses.notFound("No such entry: " + entryId);
    }
    componentClient
        .forEventSourcedEntity(SUBJECT)
        .method(CookieLogEntity::removeEntry)
        .invoke(entryId);
    return HttpResponses.ok(currentSummary());
  }

  @Get("/summary")
  public SummaryView summary() {
    return currentSummary();
  }

  @Post("/recommendation")
  public HttpResponse generateRecommendation() {
    var state = currentState();
    var summary = state.summarize();
    var profile = state.profile();
    var request =
        new DietCoachAgent.CoachRequest(
            profile.name(),
            profile.heightInches(),
            profile.concern(),
            summary.averageDailyCookies(),
            summary.daysTracked(),
            summary.peakDailyCount());

    var advice =
        componentClient
            .forAgent()
            .inSession(SUBJECT)
            .method(DietCoachAgent::recommend)
            .invoke(request);

    var recommendation =
        new DietRecommendation(
            advice.dailyCookieAllowance(),
            advice.balancingSuggestions(),
            advice.rationale(),
            LocalDate.now());
    componentClient
        .forEventSourcedEntity(SUBJECT)
        .method(CookieLogEntity::recordRecommendation)
        .invoke(recommendation);
    return HttpResponses.created(toApi(recommendation), "/api/recommendation");
  }

  @Get("/recommendation")
  public HttpResponse latestRecommendation() {
    return currentState()
        .latestRecommendation()
        .<HttpResponse>map(r -> HttpResponses.ok(toApi(r)))
        .orElseGet(() -> HttpResponses.notFound("No recommendation yet"));
  }

  // --- Helpers ---

  private CookieLog currentState() {
    return componentClient
        .forEventSourcedEntity(SUBJECT)
        .method(CookieLogEntity::getState)
        .invoke();
  }

  private SummaryView currentSummary() {
    var state = currentState();
    var today = LocalDate.now();
    var todayEntries = state.entriesForDay(today).stream().map(this::toApi).toList();
    var recentDays =
        state.recentDays(7).stream()
            .map(d -> new DailyTotalView(d.day().toString(), d.totalCount()))
            .toList();
    var recommendation = state.latestRecommendation().map(this::toApi).orElse(null);
    return new SummaryView(
        today.toString(), state.totalForDay(today), todayEntries, recentDays, recommendation);
  }

  private EntryView toApi(CookieEntry entry) {
    return new EntryView(
        entry.entryId(), entry.count(), entry.type().orElse(null), entry.day().toString());
  }

  private RecommendationView toApi(DietRecommendation r) {
    return new RecommendationView(
        r.dailyCookieAllowance(), r.balancingSuggestions(), r.rationale(), r.generatedOn().toString());
  }
}
