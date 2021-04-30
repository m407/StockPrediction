package com.isaac.stock.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;

import java.time.LocalTime;

public class TradeTimeIndicator extends AbstractIndicator<Boolean> {
  public TradeTimeIndicator(BarSeries series) {
    super(series);
  }

  @Override
  public Boolean getValue(int index) {
    LocalTime endTime = this.getBarSeries().getBar(index).getEndTime().toLocalTime();
    return endTime.isAfter(LocalTime.of(10, 30)) &&
            endTime.isBefore(LocalTime.of(18, 30));
  }
}
