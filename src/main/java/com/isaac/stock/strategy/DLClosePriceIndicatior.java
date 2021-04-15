package com.isaac.stock.strategy;

import com.isaac.stock.representation.StockData;
import com.isaac.stock.representation.StockDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;

public class DLClosePriceIndicatior extends CachedIndicator<Double> {
  private MultiLayerNetwork net;
  private StockDataSetIterator stockDataSetIterator;

  public DLClosePriceIndicatior(MultiLayerNetwork net, StockDataSetIterator stockDataSetIterator, BarSeries series) {
    super(series);

    this.net = net;
    this.stockDataSetIterator = stockDataSetIterator;
  }

  @Override
  protected Double calculate(int index) {
    try {
      Bar bar = this.getBarSeries().getBar(index);
      INDArray predicts;
      INDArray max = Nd4j.create(stockDataSetIterator.getMaxLabelArray());
      INDArray min = Nd4j.create(stockDataSetIterator.getMinLabelArray());

      StockData previosDayData = stockDataSetIterator.getStockDataReader().readOne(bar.getBeginTime().toLocalDateTime().minusDays(1), bar.getDateName());
      StockData currentDayData = stockDataSetIterator.getStockDataReader().readOne(bar.getBeginTime().toLocalDateTime(), bar.getDateName());
      INDArray testData = Nd4j.create(previosDayData.getData());
      predicts = net
              .rnnTimeStep(testData)
              .mul(max.sub(min)).add(min);

      double adjOpen = currentDayData.getData()[3];
      double adjClose = predicts.getDouble(3) + adjOpen - predicts.getDouble(0);
      return adjClose;
    } catch (Exception e) {
      return -1.0;
    }
  }
}
