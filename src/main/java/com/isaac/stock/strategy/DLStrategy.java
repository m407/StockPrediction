package com.isaac.stock.strategy;

import com.isaac.stock.representation.StockDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.trading.rules.*;

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
    Rule entryRule = new OverIndicatorRule(dlDayClosePriceIndicatior, dlDayOpenPriceIndicatior)
            .and(new OrRule(
                    new CrossedDownIndicatorRule(openPriceIndicator, dlDayOpenPriceIndicatior),
                    new UnderIndicatorRule(closePriceIndicator, dlDayOpenPriceIndicatior)));// Trend

    // Exit rule
    Rule exitRule = new CrossedUpIndicatorRule(closePriceIndicator, dlDayClosePriceIndicatior) // Trend
            .or(new TrailingStopLossRule(closePriceIndicator, DoubleNum.valueOf(0.5), 2)); // Signal 1

    return new BaseStrategy(entryRule, exitRule);
  }

  public static void printOutStrategy(MultiLayerNetwork net, StockDataSetIterator stockDataSetIterator, BarSeries series) {
    // Building the trading strategy
    Strategy strategy = buildStrategy(net, stockDataSetIterator, series);

    // Running the strategy
    BarSeriesManager seriesManager = new BarSeriesManager(series);
    TradingRecord tradingRecord = seriesManager.run(strategy, Order.OrderType.BUY);

    tradingRecord.getTrades().forEach(trade -> {
      System.out.println("Trade entry: " + trade.getEntry().toString());
      System.out.println("Trade entry: " + trade.getExit().toString());

    });
    System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());

    // Analysis
    System.out.println(
            "Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
  }
}
