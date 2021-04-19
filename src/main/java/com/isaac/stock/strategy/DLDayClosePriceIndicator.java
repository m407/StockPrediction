package com.isaac.stock.strategy;

import com.isaac.stock.representation.StockData;
import com.isaac.stock.representation.StockDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;

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

      double adjOpen = currentDayData.getData()[0];
      double adjClose = predicts.getDouble(3) + adjOpen - predicts.getDouble(0);
      return PrecisionNum.valueOf(adjClose);
    } catch (Exception e) {
      return PrecisionNum.valueOf(Double.MIN_VALUE);
    }
  }
}
