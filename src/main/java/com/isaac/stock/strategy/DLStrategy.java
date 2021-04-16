package com.isaac.stock.strategy;

import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ConstantIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

public class DLStrategy {
  public static Strategy buildStrategy(BarSeries series) {
    if (series == null) {
      throw new IllegalArgumentException("Series cannot be null");
    }

    OpenPriceIndicator openPriceIndicator = new OpenPriceIndicator(series);

    // The bias is bullish when the shorter-moving average moves above the longer
    // moving average.
    // The bias is bearish when the shorter-moving average moves below the longer
    // moving average.
    ConstantIndicator shortEma = new ConstantIndicator(series, 9.0);
    EMAIndicator longEma = new EMAIndicator(openPriceIndicator, 26);

    StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);

    MACDIndicator macd = new MACDIndicator(openPriceIndicator, 9, 26);
    EMAIndicator emaMacd = new EMAIndicator(macd, 18);

    // Entry rule
    Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
            .and(new CrossedDownIndicatorRule(stochasticOscillK, 20)) // Signal 1
            .and(new OverIndicatorRule(macd, emaMacd)); // Signal 2

    // Exit rule
    Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
            .and(new CrossedUpIndicatorRule(stochasticOscillK, 20)) // Signal 1
            .and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2

    return new BaseStrategy(entryRule, exitRule);
  }

  public static void printOutStrategy(BarSeries series) {
    // Building the trading strategy
    Strategy strategy = buildStrategy(series);

    // Running the strategy
    BarSeriesManager seriesManager = new BarSeriesManager(series);
    TradingRecord tradingRecord = seriesManager.run(strategy);
    System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());

    // Analysis
    System.out.println(
            "Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
  }
}
