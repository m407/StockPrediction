package com.isaac.stock.strategy;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.indicators.CachedIndicator;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DLDayBarPriceIndicator extends CachedIndicator<Bar> {
  private BarSeries predictSeries;

  public DLDayBarPriceIndicator(BarSeries predictSeries, BarSeries series) {
    super(series);

    this.predictSeries = predictSeries;
  }

  @Override
  protected Bar calculate(int index) {
    Bar bar = this.getBarSeries().getBar(index);
    Bar res;

    for (int i = 0; i < predictSeries.getBarCount(); i++) {
      if (predictSeries.getBar(i).getBeginTime().isBefore(bar.getBeginTime()) &&
              predictSeries.getBar(i).getEndTime().isAfter(bar.getEndTime())) {
        return predictSeries.getBar(i);
      }
    }
    res = new BaseBar(
            Duration.ofDays(1),
            ZonedDateTime.ofLocal(bar.getBeginTime().toLocalDate().atTime(0, 0), ZoneId.systemDefault(), ZoneOffset.UTC),
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            0
    );
    return res;
  }
}
