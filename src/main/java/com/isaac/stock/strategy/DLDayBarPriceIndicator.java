package com.isaac.stock.strategy;

import com.isaac.stock.predict.StockPricePrediction;
import com.isaac.stock.representation.StockDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.indicators.CachedIndicator;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DLDayBarPriceIndicator extends CachedIndicator<Bar> {
  private MultiLayerNetwork net;
  private StockDataSetIterator stockDataSetIterator;

  public DLDayBarPriceIndicator(MultiLayerNetwork net, StockDataSetIterator stockDataSetIterator, BarSeries series) {
    super(series);

    this.net = net;
    this.stockDataSetIterator = stockDataSetIterator;
  }

  @Override
  protected Bar calculate(int index) {
    Bar bar = this.getBarSeries().getBar(index);
    Bar res;
    double adjClose = Double.MIN_VALUE;
    double adjOpen = Double.MIN_VALUE;
    double adjHigh = Double.MIN_VALUE;
    double adjLow = Double.MIN_VALUE;
    INDArray predicts;
    INDArray actuals;
    INDArray max = Nd4j.create(stockDataSetIterator.getMaxLabelArray());
    INDArray min = Nd4j.create(stockDataSetIterator.getMinLabelArray());

    for (int i = 0; i < stockDataSetIterator.getTestData().size(); i++) {
      if (stockDataSetIterator.getTestData().get(i + StockPricePrediction.exampleLength).getDate().toLocalDate().isEqual(bar.getEndTime().toLocalDate())) {
        predicts = net.rnnTimeStep(stockDataSetIterator.getTestDataSet().get(i).getKey()).getRow(StockPricePrediction.exampleLength - 1).mul(max.sub(min)).add(min);
        actuals = stockDataSetIterator.getTestDataSet().get(i).getValue();
        adjOpen = actuals.getDouble(0);
        adjHigh = predicts.getDouble(1) + adjOpen - predicts.getDouble(0);
        adjLow = predicts.getDouble(2) + adjOpen - predicts.getDouble(0);
        adjClose = predicts.getDouble(3) + adjOpen - predicts.getDouble(0);
        res = new BaseBar(
                Duration.ofDays(1),
                ZonedDateTime.of(bar.getBeginTime().toLocalDate().atTime(0, 0), ZoneId.of("UTC")),
                adjOpen,
                adjHigh,
                adjLow,
                adjClose,
                0
        );
        return res;
      }
    }
    res = new BaseBar(
            Duration.ofDays(1),
            ZonedDateTime.from(bar.getBeginTime().toLocalDate()),
            adjOpen,
            adjHigh,
            adjLow,
            adjClose,
            0
    );
    return res;
  }
}
