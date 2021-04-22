package com.isaac.stock.strategy;

import com.isaac.stock.representation.StockData;
import com.isaac.stock.representation.StockDataSetIterator;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class DLDayOpenPriceIndicator extends CachedIndicator<Num> {
  private StockDataSetIterator stockDataSetIterator;

  public DLDayOpenPriceIndicator(StockDataSetIterator stockDataSetIterator, BarSeries series) {
    super(series);

    this.stockDataSetIterator = stockDataSetIterator;
  }

  @Override
  protected Num calculate(int index) {
    try {
      Bar bar = this.getBarSeries().getBar(index);
      StockData currentDayData = stockDataSetIterator.getStockDataReader().readOne(bar.getBeginTime().toLocalDateTime(), "D");
      return DoubleNum.valueOf(currentDayData.getData()[0]);
    } catch (Exception e) {
      return DoubleNum.valueOf(Double.MIN_VALUE);
    }
  }
}
