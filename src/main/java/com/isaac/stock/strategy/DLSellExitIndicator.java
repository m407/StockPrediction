package com.isaac.stock.strategy;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class DLSellExitIndicator extends CachedIndicator<Num> {
  private DLDayBarPriceIndicator dlDayBarPriceIndicator;
  private double threshhold;

  public DLSellExitIndicator(DLDayBarPriceIndicator dlDayBarPriceIndicator, BarSeries series, double threshhold) {
    super(series);
    this.dlDayBarPriceIndicator = dlDayBarPriceIndicator;
    this.threshhold = threshhold;
  }

  @Override
  protected Num calculate(int index) {
    Bar bar = dlDayBarPriceIndicator.getValue(index);
    if (bar.getClosePrice().isLessThanOrEqual(bar.getOpenPrice())) {
      return bar
              .getClosePrice()
              .minus(
                      bar.getOpenPrice()
                              .minus(bar.getClosePrice())
                              .multipliedBy(DoubleNum.valueOf(threshhold)));
    } else {
      return DoubleNum.valueOf(Double.MAX_VALUE);
    }
  }
}
