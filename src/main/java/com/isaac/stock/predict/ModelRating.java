package com.isaac.stock.predict;

public class ModelRating {
  private double averageAdjusted;
  private double overlapPercent;
  private double floorAverageAdjusted;

  public ModelRating() {
    this.setAverageAdjusted(0);
    this.setOverlapPercent(0);
  }

  public double getAverageAdjusted() {
    return averageAdjusted;
  }

  public void setAverageAdjusted(double averageAdjusted) {
    this.averageAdjusted = averageAdjusted;
    floorAverageAdjusted = Math.floor(averageAdjusted);
  }

  public double getOverlapPercent() {
    return overlapPercent;
  }

  public void setOverlapPercent(double overlapPercent) {
    this.overlapPercent = Math.floor(overlapPercent);
  }

  public double getFloorAverageAdjusted() {
    return floorAverageAdjusted;
  }
}
