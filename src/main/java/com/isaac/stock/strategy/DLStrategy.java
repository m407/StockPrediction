package com.isaac.stock.strategy;

import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.pnl.GrossLossCriterion;
import org.ta4j.core.analysis.criteria.pnl.GrossProfitCriterion;
import org.ta4j.core.analysis.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.*;

public class DLStrategy {
  public static Strategy buildBuyStrategy(BarSeries predictSeries, BarSeries series) {
    if (series == null) {
      throw new IllegalArgumentException("Series cannot be null");
    }

    OpenPriceIndicator openPriceIndicator = new OpenPriceIndicator(series);
    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
    LowPriceIndicator lowPriceIndicator = new LowPriceIndicator(series);
    HighPriceIndicator highPriceIndicator = new HighPriceIndicator(series);
    DLDayBarPriceIndicator dlDayBarPriceIndicator = new DLDayBarPriceIndicator(predictSeries, series);
    DLDayOpenPriceIndicator dlDayOpenPriceIndicatior = new DLDayOpenPriceIndicator(dlDayBarPriceIndicator, series);
    DLDayClosePriceIndicator dlDayClosePriceIndicatior = new DLDayClosePriceIndicator(dlDayBarPriceIndicator, series);

    DLBuyEnterIndicator dlBuyEnterIndicator = new DLBuyEnterIndicator(dlDayBarPriceIndicator, series, 0);
    DLBuyEnterIndicator dlStopLossIndicator = new DLBuyEnterIndicator(dlDayBarPriceIndicator, series, 4.236 * 2.618);
    DLBuyExitIndicator dlBuyExitIndicator = new DLBuyExitIndicator(dlDayBarPriceIndicator, series, 0.382);

    // Entry rule
    Rule entryRule = new BooleanIndicatorRule(new TradeTimeIndicator(series)) // Время торговли
            .and(new OverIndicatorRule(dlDayClosePriceIndicatior, dlDayOpenPriceIndicatior))
            .and(new OrRule(
                            new UnderIndicatorRule(lowPriceIndicator, dlBuyEnterIndicator),
                            new UnderIndicatorRule(closePriceIndicator, dlBuyEnterIndicator)
                    )
                            .or(new UnderIndicatorRule(openPriceIndicator, dlBuyEnterIndicator))
                            .or(new UnderIndicatorRule(highPriceIndicator, dlBuyEnterIndicator))
            )
            .and(new OverIndicatorRule(lowPriceIndicator, dlStopLossIndicator));

    // Exit rule
    Rule exitRule = new NotRule(new BooleanIndicatorRule(new TradeTimeIndicator(series)))
            .or(new OrRule(
                            new UnderIndicatorRule(lowPriceIndicator, dlStopLossIndicator),
                            new UnderIndicatorRule(closePriceIndicator, dlStopLossIndicator)
                    )
                            .or(new UnderIndicatorRule(openPriceIndicator, dlStopLossIndicator))
                            .or(new UnderIndicatorRule(highPriceIndicator, dlStopLossIndicator))
            )
            .or(new AndRule(
                    new OverIndicatorRule(highPriceIndicator, dlBuyExitIndicator),
                    new TrailingStopLossRule(closePriceIndicator, DoubleNum.valueOf(0.1), 2)
            ));

    return new BaseStrategy(entryRule, exitRule);
  }

  public static Strategy buildSellStrategy(BarSeries predictSeries, BarSeries series) {
    if (series == null) {
      throw new IllegalArgumentException("Series cannot be null");
    }

    OpenPriceIndicator openPriceIndicator = new OpenPriceIndicator(series);
    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
    LowPriceIndicator lowPriceIndicator = new LowPriceIndicator(series);
    HighPriceIndicator highPriceIndicator = new HighPriceIndicator(series);
    DLDayBarPriceIndicator dlDayBarPriceIndicator = new DLDayBarPriceIndicator(predictSeries, series);
    DLDayOpenPriceIndicator dlDayOpenPriceIndicatior = new DLDayOpenPriceIndicator(dlDayBarPriceIndicator, series);
    DLDayClosePriceIndicator dlDayClosePriceIndicatior = new DLDayClosePriceIndicator(dlDayBarPriceIndicator, series);

    DLSellEnterIndicator dlSellEnterIndicator = new DLSellEnterIndicator(dlDayBarPriceIndicator, series, 0);
    DLSellEnterIndicator dlStopLossIndicator = new DLSellEnterIndicator(dlDayBarPriceIndicator, series, -4.236 * 2.618);
    DLSellExitIndicator dlBuyExitIndicator = new DLSellExitIndicator(dlDayBarPriceIndicator, series, 0.382);

    // Entry rule
    Rule entryRule = new BooleanIndicatorRule(new TradeTimeIndicator(series)) // Время торговли
            .and(new UnderIndicatorRule(dlDayClosePriceIndicatior, dlDayOpenPriceIndicatior))
            .and(new OrRule(
                            new OverIndicatorRule(lowPriceIndicator, dlSellEnterIndicator),
                            new OverIndicatorRule(closePriceIndicator, dlSellEnterIndicator)
                    )
                            .or(new OverIndicatorRule(openPriceIndicator, dlSellEnterIndicator))
                            .or(new OverIndicatorRule(highPriceIndicator, dlSellEnterIndicator))
            )
            .and(new UnderIndicatorRule(lowPriceIndicator, dlStopLossIndicator));

    // Exit rule
    Rule exitRule = new NotRule(new BooleanIndicatorRule(new TradeTimeIndicator(series)))
            .or(new OrRule(
                            new OverIndicatorRule(lowPriceIndicator, dlStopLossIndicator),
                            new OverIndicatorRule(closePriceIndicator, dlStopLossIndicator)
                    )
                            .or(new OverIndicatorRule(openPriceIndicator, dlStopLossIndicator))
                            .or(new OverIndicatorRule(highPriceIndicator, dlStopLossIndicator))
            )
            .or(new AndRule(
                    new UnderIndicatorRule(highPriceIndicator, dlBuyExitIndicator),
                    new TrailingStopLossRule(closePriceIndicator, DoubleNum.valueOf(0.1), 2)
            ));

    return new BaseStrategy(entryRule, exitRule);
  }

  public static TradingRecord runStrategy(BarSeries predictSeries, BarSeries series) {
    // Building the trading strategy
//    Strategy strategy = buildBuyStrategy(predictSeries, series);
    Strategy strategy = buildSellStrategy(predictSeries, series);

    // Running the strategy
    BarSeriesManager seriesManager = new BarSeriesManager(series);
    TradingRecord tradingRecord = seriesManager.run(strategy, Trade.TradeType.SELL);

    GrossReturnCriterion totalReturn = new GrossReturnCriterion();
    GrossProfitCriterion grossProfit = new GrossProfitCriterion();
    GrossLossCriterion grossLoss = new GrossLossCriterion();
    System.out.println("Total return: " + totalReturn.calculate(series, tradingRecord));
    System.out.println("Total profit: " + grossProfit.calculate(series, tradingRecord));
    System.out.println("Total loss: " + grossLoss.calculate(series, tradingRecord));
    System.out.println("Number of trades for the strategy: " + tradingRecord.getPositions().size());
    return tradingRecord;
  }
}
