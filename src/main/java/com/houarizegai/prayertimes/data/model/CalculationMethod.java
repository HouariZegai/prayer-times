package com.houarizegai.prayertimes.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CalculationMethod {
  private int id;
  private String name;
  private String nameAr;
}
