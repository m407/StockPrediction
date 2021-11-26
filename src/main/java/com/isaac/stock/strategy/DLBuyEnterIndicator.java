package com.isaac.stock.strategy;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class DLBuyEnterIndicator extends CachedIndicator<Num> {
  private DLDayBarPriceIndicator dlDayBarPriceIndicator;
  private double threshhold;

  public DLBuyEnterIndicator(DLDayBarPriceIndicator dlDayBarPriceIndicator, BarSeries series, double threshhold) {
    super(series);
    this.dlDayBarPriceIndicator = dlDayBarPriceIndicator;
    this.threshhold = threshhold;
  }

  @Override
  protected Num calculate(int index) {
    Bar bar = dlDayBarPriceIndicator.getValue(index);
    if (bar.getClosePrice().isGreaterThanOrEqual(bar.getOpenPrice())) {
      return bar
              .getOpenPrice()
              .minus(
                      bar.getClosePrice()
                              .minus(bar.getOpenPrice())
                              .multipliedBy(DoubleNum.valueOf(threshhold)));
    } else {
      return DoubleNum.valueOf(Double.MIN_VALUE);
    }
  }
}
