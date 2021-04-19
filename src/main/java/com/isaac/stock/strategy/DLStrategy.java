package com.isaac.stock.strategy;

import com.isaac.stock.representation.StockDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.TrailingStopLossRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

public class DLStrategy {
  public static Strategy buildStrategy(MultiLayerNetwork net, StockDataSetIterator stockDataSetIterator, BarSeries series) {
    if (series == null) {
      throw new IllegalArgumentException("Series cannot be null");
    }

    OpenPriceIndicator openPriceIndicator = new OpenPriceIndicator(series);
    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
    DLDayOpenPriceIndicator dlDayOpenPriceIndicatior = new DLDayOpenPriceIndicator(stockDataSetIterator, series);
    DLDayClosePriceIndicator dlDayClosePriceIndicatior = new DLDayClosePriceIndicator(net, stockDataSetIterator, series);

    // Entry rule
    Rule entryRule = new OverIndicatorRule(dlDayClosePriceIndicatior, dlDayOpenPriceIndicatior) // Trend
            .and(new CrossedDownIndicatorRule(dlDayOpenPriceIndicatior, 20)) // Signal 1
            .and(new UnderIndicatorRule(dlDayOpenPriceIndicatior, openPriceIndicator)); // Signal 2

    // Exit rule
    Rule exitRule = new UnderIndicatorRule(dlDayClosePriceIndicatior, closePriceIndicator) // Trend
            .or(new TrailingStopLossRule(closePriceIndicator, DoubleNum.valueOf(0.5), 2)); // Signal 1

    return new BaseStrategy(entryRule, exitRule);
  }

  public static void printOutStrategy(MultiLayerNetwork net, StockDataSetIterator stockDataSetIterator, BarSeries series) {
    // Building the trading strategy
    Strategy strategy = buildStrategy(net, stockDataSetIterator, series);

    // Running the strategy
    BarSeriesManager seriesManager = new BarSeriesManager(series);
    TradingRecord tradingRecord = seriesManager.run(strategy, Order.OrderType.BUY);
    System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());

    // Analysis
    System.out.println(
            "Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
  }
}
