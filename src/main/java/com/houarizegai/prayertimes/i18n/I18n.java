package com.houarizegai.prayertimes.i18n;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class I18n {

  public static final String LANG_AR = "ar";
  public static final String LANG_EN = "en";

  private static JSONObject bundle = new JSONObject();
  private static String lang = LANG_EN;

  private I18n() {
  }

  public static void init(String language) {
    lang = (language == null || language.isBlank()) ? LANG_EN : language;
    JSONObject loaded = loadBundle(lang);
    if (loaded == null && !LANG_EN.equals(lang)) {
      loaded = loadBundle(LANG_EN);
    }
    if (loaded != null) {
      bundle = loaded;
    }
  }

  private static JSONObject loadBundle(String language) {
    try (InputStream in = I18n.class.getResourceAsStream("/i18n/" + language + ".json")) {
      if (in == null) {
        return null;
      }
      return new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      return null;
    }
  }

  public static String lang() {
    return lang;
  }

  public static boolean isRtl() {
    return LANG_AR.equals(lang);
  }

  public static String t(String key) {
    return bundle.optString(key, key);
  }

  public static String t(String key, String arg0) {
    return t(key).replace("{0}", arg0);
  }

  public static String t(String key, String arg0, String arg1) {
    return t(key, arg0).replace("{1}", arg1);
  }
}
