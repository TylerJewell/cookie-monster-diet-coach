package com.example.domain;

/** Fixed context for the single tracked subject. */
public record CookieMonsterProfile(String name, int heightInches, String concern) {

  public static CookieMonsterProfile defaultProfile() {
    return new CookieMonsterProfile("Cookie Monster", 60, "cholesterol");
  }
}
