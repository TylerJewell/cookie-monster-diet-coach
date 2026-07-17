package com.example.api;

import static org.assertj.core.api.Assertions.assertThat;

import akka.javasdk.testkit.TestKitSupport;
import org.junit.jupiter.api.Test;

public class CookieDietEndpointIntegrationTest extends TestKitSupport {

  private int todayTotal() {
    return httpClient
        .GET("/api/summary")
        .responseBodyAs(CookieDietEndpoint.SummaryView.class)
        .invoke()
        .body()
        .todayTotal();
  }

  @Test
  public void loggingCookiesIncreasesTodayTotal() {
    int before = todayTotal();

    var response =
        httpClient
            .POST("/api/cookies")
            .withRequestBody(new CookieDietEndpoint.LogCookieRequest(4, "chocolate chip"))
            .responseBodyAs(CookieDietEndpoint.SummaryView.class)
            .invoke();

    assertThat(response.status().isSuccess()).isTrue();
    assertThat(response.body().todayTotal() - before).isEqualTo(4);
  }

  @Test
  public void rejectsNonPositiveCount() {
    var response =
        httpClient
            .POST("/api/cookies")
            .withRequestBody(new CookieDietEndpoint.LogCookieRequest(0, null))
            .invoke();

    assertThat(response.status().intValue()).isEqualTo(400);
  }

  @Test
  public void generatesRecommendationWithNonZeroAllowance() {
    httpClient
        .POST("/api/cookies")
        .withRequestBody(new CookieDietEndpoint.LogCookieRequest(8, null))
        .invoke();

    var response =
        httpClient
            .POST("/api/recommendation")
            .responseBodyAs(CookieDietEndpoint.RecommendationView.class)
            .invoke();

    assertThat(response.status().isSuccess()).isTrue();
    assertThat(response.body().dailyCookieAllowance()).isGreaterThan(0);
  }
}
