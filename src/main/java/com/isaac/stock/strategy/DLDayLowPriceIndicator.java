package com.isaac.stock.strategy;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class DLDayLowPriceIndicator extends CachedIndicator<Num> {
  private DLDayBarPriceIndicator dlDayBarPriceIndicator;

  public DLDayLowPriceIndicator(DLDayBarPriceIndicator dlDayBarPriceIndicator, BarSeries series) {
    super(series);
    this.dlDayBarPriceIndicator = dlDayBarPriceIndicator;
  }

  @Override
  protected Num calculate(int index) {
    Bar bar = dlDayBarPriceIndicator.getValue(index);
    return bar.getLowPrice();
  }
}