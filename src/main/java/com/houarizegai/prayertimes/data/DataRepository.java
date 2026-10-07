package com.houarizegai.prayertimes.data;

import com.houarizegai.prayertimes.data.model.CalculationMethod;
import com.houarizegai.prayertimes.data.model.City;
import com.houarizegai.prayertimes.data.model.Country;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class DataRepository {

  private DataRepository() {
  }

  public static List<Country> loadCountries() {
    JSONObject root = new JSONObject(readResource("/data/locations.json"));
    JSONArray countriesJson = root.getJSONArray("countries");
    List<Country> countries = new ArrayList<>();

    for (int i = 0; i < countriesJson.length(); i++) {
      JSONObject countryJson = countriesJson.getJSONObject(i);
      JSONObject nameJson = countryJson.getJSONObject("name");
      JSONArray citiesJson = countryJson.getJSONArray("cities");
      List<City> cities = new ArrayList<>();

      for (int j = 0; j < citiesJson.length(); j++) {
        JSONObject cityJson = citiesJson.getJSONObject(j);
        cities.add(City.builder()
          .name(cityJson.getString("name"))
          .nameAr(cityJson.optString("nameAr", null))
          .lat(cityJson.getDouble("lat"))
          .lng(cityJson.getDouble("lng"))
          .build());
      }

      countries.add(Country.builder()
        .code(countryJson.getString("code"))
        .nameEn(nameJson.getString("en"))
        .nameAr(nameJson.getString("ar"))
        .cities(cities)
        .build());
    }

    return countries;
  }

  public static List<CalculationMethod> loadMethods() {
    JSONArray methodsJson = new JSONArray(readResource("/data/methods.json"));
    List<CalculationMethod> methods = new ArrayList<>();

    for (int i = 0; i < methodsJson.length(); i++) {
      JSONObject methodJson = methodsJson.getJSONObject(i);
      methods.add(CalculationMethod.builder()
        .id(methodJson.getInt("id"))
        .name(methodJson.getString("name"))
        .nameAr(methodJson.getString("nameAr"))
        .build());
    }

    return methods;
  }

  private static String readResource(String path) {
    try (InputStream in = DataRepository.class.getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalStateException("Missing resource: " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read resource: " + path, e);
    }
  }
}
