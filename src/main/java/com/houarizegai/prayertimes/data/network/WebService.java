package com.houarizegai.prayertimes.data.network;

import com.houarizegai.prayertimes.data.model.PrayerTimes;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class WebService {

  private static final String PRAYER_TIMES_END_POINT = "http://api.aladhan.com/v1/timingsByCity";
  private static final Logger LOG = Logger.getLogger(WebService.class.getName());

  public PrayerTimes getPrayerTimes(String city) {
    try {
      // Get the current date
      LocalDate currentDate = LocalDate.now();

      // Define the date format
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

      // Format the current date
      String formattedDate = currentDate.format(formatter);
      
      // Get the prayer times from the API
      HttpResponse<JsonNode> jsonResponse = Unirest.get(PRAYER_TIMES_END_POINT + "/" + formattedDate)
        .queryString("city", city)
        .queryString("country", "Algeria")
        .queryString("method", "8")
        .asJson();

      JSONObject jsonRoot = new JSONObject(jsonResponse.getBody().toString());

      if (jsonRoot.has("code") && jsonRoot.getInt("code") == 200 ) { // Is city founded?
        JSONObject prayerTimes = jsonRoot.getJSONObject("data").getJSONObject("timings");

        return PrayerTimes.builder()
          .fajr(prayerTimes.getString("Fajr"))
          .sunrise(prayerTimes.getString("Sunrise"))
          .dhuhr(prayerTimes.getString("Dhuhr"))
          .asr(prayerTimes.getString("Asr"))
          .maghrib(prayerTimes.getString("Maghrib"))
          .isha(prayerTimes.getString("Isha"))
          .build();
      }
    } catch (UnirestException e) {
      LOG.warning(e.getMessage());
    }

    return getDefaultPrayerTimes();
  }

  private PrayerTimes getDefaultPrayerTimes() {
    String defaultTime = "--:--";
    return PrayerTimes.builder()
      .fajr(defaultTime)
      .sunrise(defaultTime)
      .dhuhr(defaultTime)
      .asr(defaultTime)
      .maghrib(defaultTime)
      .isha(defaultTime)
      .build();
  }
}
