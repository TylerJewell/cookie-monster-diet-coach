package com.example.application;

import static org.assertj.core.api.Assertions.assertThat;

import akka.javasdk.testkit.TestKitSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Exercises the real {@link DietCoachAgent} against its offline {@link MockDietModelProvider}
 * (no API key, no network). The advice is deterministic, so no {@code TestModelProvider} is needed.
 */
public class DietCoachAgentTest extends TestKitSupport {

  @Test
  public void producesNonZeroAllowanceAndAddressesCholesterol() {
    var request = new DietCoachAgent.CoachRequest("Cookie Monster", 60, "cholesterol", 10.0, 3, 12);

    var advice =
        componentClient
            .forAgent()
            .inSession(UUID.randomUUID().toString())
            .method(DietCoachAgent::recommend)
            .invoke(request);

    assertThat(advice.dailyCookieAllowance()).isGreaterThan(0);
    assertThat(advice.balancingSuggestions()).isNotEmpty();
    assertThat(advice.rationale().toLowerCase()).contains("cholesterol");
  }
}
