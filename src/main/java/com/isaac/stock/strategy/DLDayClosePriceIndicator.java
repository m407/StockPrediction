package com.isaac.stock.strategy;

import com.isaac.stock.predict.StockPricePrediction;
import com.isaac.stock.representation.StockDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class DLDayClosePriceIndicator extends CachedIndicator<Num> {
  private MultiLayerNetwork net;
  private StockDataSetIterator stockDataSetIterator;

  public DLDayClosePriceIndicator(MultiLayerNetwork net, StockDataSetIterator stockDataSetIterator, BarSeries series) {
    super(series);

    this.net = net;
    this.stockDataSetIterator = stockDataSetIterator;
  }

  @Override
  protected Num calculate(int index) {
    Bar bar = this.getBarSeries().getBar(index);
    double adjClose = Double.MIN_VALUE;
    INDArray predicts;
    INDArray actuals;
    INDArray max = Nd4j.create(stockDataSetIterator.getMaxLabelArray());
    INDArray min = Nd4j.create(stockDataSetIterator.getMinLabelArray());

    for (int i = 0; i < stockDataSetIterator.getTestData().size(); i++) {
      if (stockDataSetIterator.getTestData().get(i + StockPricePrediction.exampleLength).getDate().toLocalDate().isEqual(bar.getEndTime().toLocalDate())) {
        predicts = net.rnnTimeStep(stockDataSetIterator.getTestDataSet().get(i).getKey()).getRow(StockPricePrediction.exampleLength - 1).mul(max.sub(min)).add(min);
        actuals = stockDataSetIterator.getTestDataSet().get(i).getValue();
        double adjOpen = actuals.getDouble(0);
        adjClose = predicts.getDouble(3) + adjOpen - predicts.getDouble(0);
        return DoubleNum.valueOf(adjClose);
      }
    }
    return DoubleNum.valueOf(adjClose);
  }
}
