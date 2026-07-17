package com.example.application;

import akka.javasdk.JsonSupport;
import akka.javasdk.agent.ModelProvider;
import com.example.domain.CookieMonsterProfile;
import com.example.domain.DietRecommendation;
import com.example.domain.DietRecommender;
import com.example.domain.IntakeSummary;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * A mock LLM for the prototype. Implements a LangChain4j {@link ChatModel} that never calls any
 * real model provider and never reads an API key. It parses the intake data out of the agent's
 * user message and returns deterministic {@link DietCoachAgent.CoachAdvice} as JSON, so
 * {@link DietCoachAgent} exercises the full structured-response flow offline at zero cost.
 */
public class MockDietModelProvider implements ModelProvider.Custom {

  @Override
  public String modelName() {
    return "mock-diet-coach";
  }

  @Override
  public Object createChatModel() {
    return new ChatModel() {
      @Override
      public ChatResponse chat(ChatRequest request) {
        var advice = adviseFrom(lastUserText(request.messages()));
        return ChatResponse.builder()
            .aiMessage(AiMessage.from(JsonSupport.encodeToString(advice)))
            .metadata(
                ChatResponseMetadata.builder()
                    .id("mock-diet-coach-response")
                    .modelName("mock-diet-coach")
                    .tokenUsage(new TokenUsage(0, 0))
                    .finishReason(FinishReason.STOP)
                    .build())
            .build();
      }
    };
  }

  @Override
  public Object createStreamingChatModel() {
    throw new UnsupportedOperationException("Mock diet model does not support streaming");
  }

  private static String lastUserText(List<ChatMessage> messages) {
    for (int i = messages.size() - 1; i >= 0; i--) {
      if (messages.get(i) instanceof UserMessage userMessage && userMessage.hasSingleText()) {
        return userMessage.singleText();
      }
    }
    return "";
  }

  private static DietCoachAgent.CoachAdvice adviseFrom(String userJson) {
    var request =
        JsonSupport.decodeJson(
            DietCoachAgent.CoachRequest.class, userJson.getBytes(StandardCharsets.UTF_8));
    DietRecommendation recommendation =
        DietRecommender.recommend(
            new IntakeSummary(
                request.averageDailyCookies(), request.daysTracked(), request.peakDailyCount()),
            new CookieMonsterProfile(request.subjectName(), request.heightInches(), request.concern()),
            LocalDate.now());
    return new DietCoachAgent.CoachAdvice(
        recommendation.dailyCookieAllowance(),
        recommendation.balancingSuggestions(),
        recommendation.rationale());
  }
}
