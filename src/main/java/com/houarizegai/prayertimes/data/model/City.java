package com.houarizegai.prayertimes.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class City {
  private String name;
  private String nameAr;
  private double lat;
  private double lng;
}
