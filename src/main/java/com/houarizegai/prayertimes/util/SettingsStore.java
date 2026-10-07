package com.houarizegai.prayertimes.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * User settings stored in one file: read AND written on disk (src/main/resources/config),
 * so changes take effect on the next start without a rebuild. The classpath copy is only
 * a fallback when the disk file does not exist.
 */
public class SettingsStore {

  private static final Path SETTINGS_FILE =
    FileUtils.RESOURCES_PATH.resolve("config").resolve("settings.properties");

  private final Properties props = new Properties();

  public void load() {
    if (Files.isRegularFile(SETTINGS_FILE)) {
      try (InputStream in = Files.newInputStream(SETTINGS_FILE)) {
        props.load(in);
        return;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Fallback: classpath defaults (target/classes copy)
    try (InputStream in = SettingsStore.class.getResourceAsStream("/config/settings.properties")) {
      if (in != null) {
        props.load(in);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String get(String key, String defaultValue) {
    String value = props.getProperty(key);
    return (value == null || value.isBlank()) ? defaultValue : value;
  }

  public int getInt(String key, int defaultValue) {
    try {
      return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public void set(String key, String value) {
    props.setProperty(key, value);
  }

  public void save() {
    try {
      Files.createDirectories(SETTINGS_FILE.getParent());
      try (OutputStream out = Files.newOutputStream(SETTINGS_FILE)) {
        props.store(out, null);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
