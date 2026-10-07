package com.houarizegai.prayertimes.data.network;

import com.houarizegai.prayertimes.data.model.PrayerTimes;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class WebService {

  private static final String PRAYER_TIMES_END_POINT = "https://api.aladhan.com/v1/timings/";
  private static final Logger LOG = Logger.getLogger(WebService.class.getName());

  /**
   * Returns the prayer times for the given coordinates/method, or {@code null}
   * when the fetch fails (network error, bad response, non-200 API code).
   * Never throws.
   */
  public PrayerTimes getPrayerTimes(double latitude, double longitude, int method) {
    try {
      // Get the current date
      LocalDate currentDate = LocalDate.now();

      // Define the date format
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

      // Format the current date
      String formattedDate = currentDate.format(formatter);

      // Get the prayer times from the API (coordinates + calculation method)
      HttpResponse<JsonNode> jsonResponse = Unirest.get(PRAYER_TIMES_END_POINT + formattedDate)
        .queryString("latitude", latitude)
        .queryString("longitude", longitude)
        .queryString("method", method)
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
    } catch (Exception e) { // network errors, malformed responses, ...
      LOG.warning("Failed to fetch prayer times: " + e.getMessage());
    }

    return null;
  }
}
