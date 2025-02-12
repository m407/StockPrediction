package com.isaac.stock.strategy;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class DLBuyExitIndicator extends CachedIndicator<Num> {
  private DLDayBarPriceIndicator dlDayBarPriceIndicator;
  private double threshhold;

  public DLBuyExitIndicator(DLDayBarPriceIndicator dlDayBarPriceIndicator, BarSeries series, double threshhold) {
    super(series);
    this.dlDayBarPriceIndicator = dlDayBarPriceIndicator;
    this.threshhold = threshhold;
  }

  @Override
  protected Num calculate(int index) {
    Bar bar = dlDayBarPriceIndicator.getValue(index);
    if (bar.getClosePrice().isGreaterThanOrEqual(bar.getOpenPrice())) {
      return bar
              .getClosePrice()
              .plus(
                      bar.getClosePrice()
                              .minus(bar.getOpenPrice())
                              .multipliedBy(DoubleNum.valueOf(threshhold)));
    } else {
      return DoubleNum.valueOf(Double.MAX_VALUE);
    }
  }
}
