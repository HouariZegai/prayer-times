package com.houarizegai.prayertimes.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class Country {
  private String code;
  private String nameEn;
  private String nameAr;
  private List<City> cities;
}
