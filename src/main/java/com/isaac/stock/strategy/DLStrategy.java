package com.isaac.stock.strategy;

import com.isaac.stock.representation.StockDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.pnl.GrossProfitCriterion;
import org.ta4j.core.analysis.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.*;

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
    Rule entryRule = new AndRule(
            new BooleanIndicatorRule(new TradeTimeIndicator(series)),
            new OverIndicatorRule(dlDayClosePriceIndicatior, dlDayOpenPriceIndicatior)
                    .and(new OrRule(
                                    new CrossedDownIndicatorRule(openPriceIndicator, dlDayOpenPriceIndicatior),
                                    new UnderIndicatorRule(closePriceIndicator, dlDayOpenPriceIndicatior)
                            )
                    )// Trend
    );

    // Exit rule
    Rule exitRule = new NotRule(new BooleanIndicatorRule(new TradeTimeIndicator(series)))
            .or(
                    new CrossedUpIndicatorRule(closePriceIndicator, dlDayClosePriceIndicatior) // Trend
                            .or(new TrailingStopLossRule(closePriceIndicator, DoubleNum.valueOf(0.5), 2)) // Signal 1

            );

    return new BaseStrategy(entryRule, exitRule);
  }

  public static void printOutStrategy(MultiLayerNetwork net, StockDataSetIterator stockDataSetIterator, BarSeries series) {
    // Building the trading strategy
    Strategy strategy = buildStrategy(net, stockDataSetIterator, series);

    // Running the strategy
    BarSeriesManager seriesManager = new BarSeriesManager(series);
    TradingRecord tradingRecord = seriesManager.run(strategy, Trade.TradeType.BUY);

    GrossReturnCriterion totalReturn = new GrossReturnCriterion();
    GrossProfitCriterion grossProfit = new GrossProfitCriterion();
    System.out.println("Total return: " + totalReturn.calculate(series, tradingRecord));
    System.out.println("Total profit: " + grossProfit.calculate(series, tradingRecord));
    System.out.println("Number of trades for the strategy: " + tradingRecord.getPositions().size());
  }
}
