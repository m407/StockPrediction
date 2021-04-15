package com.isaac.stock.predict;

public class ModelRating {
  public double averageAdjusted;
  public double overlapPercent;
  public double floorOverlapAverage;

  public ModelRating() {
    this.averageAdjusted = 0;
    this.overlapPercent = 0;
    this.floorOverlapAverage = 0;
  }
}
