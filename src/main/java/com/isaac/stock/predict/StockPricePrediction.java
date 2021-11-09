package com.isaac.stock.predict;

import com.isaac.stock.model.RecurrentNets;
import com.isaac.stock.representation.StockData;
import com.isaac.stock.representation.StockDataReader;
import com.isaac.stock.representation.StockDataSetIterator;
import com.isaac.stock.strategy.DLStrategy;
import com.isaac.stock.utils.PlotUtil;
import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by zhanghao on 26/7/17.
 * Modified by zhanghao on 28/9/17.
 *
 * @author ZHANG HAO
 */
public class StockPricePrediction {

  private static final Logger log = LoggerFactory.getLogger(StockPricePrediction.class);

  public static final int exampleLength = 66; // time series length, assume 66 working days in 3 months

  private static INDArray[] predicts;
  private static INDArray[] actuals;
  private static BarSeries predictSeries;
  private static TradingRecord tradingRecord;

  public static void main(String[] args) throws IOException {
    String ticker = System.getProperty("prices.ticker", "RI.RTSI");
    int iterations = Integer.parseInt(System.getProperty("iterations", "1"));
    int lstmLayer1Size = Integer.parseInt(System.getProperty("lstmLayer1Size", "256"));
    int lstmLayerRatio = Integer.parseInt(System.getProperty("lstmLayerRatio", "2"));
    int batchSize = Integer.parseInt(System.getProperty("batchSize", "86")); // mini-batch size
    double splitRatio = 0.85; // 85% for training, 15% for testing
    int epochs = Integer.parseInt(System.getProperty("epochs", "8192"));
    boolean continueTraining = Boolean.parseBoolean(System.getProperty("continueTraining", "true"));

    ModelRating currentModelRating = new ModelRating();
    MultiLayerNetwork net;

    log.info("Create dataSet iterator...");
    StockDataSetIterator iterator = new StockDataSetIterator(ticker, batchSize, splitRatio);
    log.info("Load test dataset...");
    List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();


    String defaultNetworkFileName = "StockPriceLSTM_" + ticker + ".b" + batchSize + ".i" + iterations + ".lstm" + lstmLayer1Size + "x" + lstmLayerRatio;
    String multiLayerNetworkFileName = System.getProperty("network.file", defaultNetworkFileName);
    multiLayerNetworkFileName = multiLayerNetworkFileName.replaceFirst("\\.zip", "");
    File locationToSave = new File(multiLayerNetworkFileName + ".zip");

    INDArray max = Nd4j.create(iterator.getMaxLabelArray());
    INDArray min = Nd4j.create(iterator.getMinLabelArray());

    if (locationToSave.isFile() && locationToSave.exists()) {
      log.info("Load model...");
      net = ModelSerializer.restoreMultiLayerNetwork(locationToSave);
      net.setListeners(new ScoreIterationListener(100));
      log.info("Testing...");
      currentModelRating = getModelRating(net, iterator, max, min);
      if (Boolean.parseBoolean(System.getProperty("demoTrade"))) {
        StockDataReader stockDataReader = new StockDataReader("RI.RTSI.10");
        List<StockData> stockData = stockDataReader.readAll();
        BaseBarSeriesBuilder barSeriesBuilder = new BaseBarSeriesBuilder();
        BarSeries barSeries = barSeriesBuilder
                .withName("RI.RTSI.10")
                .withBars(stockData.stream()
                        .filter(item -> (item.getDate().isAfter(iterator.getTestFirstDay()) ||
                                item.getDate().isEqual(iterator.getTestFirstDay())) &&
                                (item.getDate().isBefore(iterator.getTestLastDay()) ||
                                        item.getDate().isEqual(iterator.getTestLastDay()))
                        )
                        .map(item -> new BaseBar(
                                Duration.ofMinutes(10),
                                ZonedDateTime.ofLocal(item.getDate(), ZoneId.systemDefault(), ZoneOffset.UTC),
                                item.getData()[0],
                                item.getData()[1],
                                item.getData()[2],
                                item.getData()[3],
                                item.getData()[4]
                        ))
                        .collect(Collectors.toList()))
                .build();
        tradingRecord = DLStrategy.runStrategy(predictSeries, barSeries);
      }
      if (Boolean.parseBoolean(System.getProperty("plot"))) {
        plot(iterator, multiLayerNetworkFileName, tradingRecord);
      }
    } else {
      log.info("Build lstm networks...");
      net = RecurrentNets.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes());
    }
    if (continueTraining) {
      // saveUpdater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this to train your network more in the future
      log.info("Training...");
      for (int i = 0; i < epochs; i++) {
        while (iterator.hasNext()) net.fit(iterator.next()); // fit model using mini-batch data
        iterator.reset(); // reset iterator
        net.rnnClearPreviousState(); // clear previous state
        ModelRating modelRating = getModelRating(net, iterator, max, min);
        if (modelRating.getAverageAdjusted() > 4 || modelRating.getOverlapPercent() > 58 || modelRating.getAverageAdjusted() > currentModelRating.getAverageAdjusted()) {
          currentModelRating = modelRating;
          File saveTemp = new File(multiLayerNetworkFileName + ".rating" + modelRating.getFloorAverageAdjusted() + ".percent" + modelRating.getOverlapPercent() + "." + i + ".zip");
          ModelSerializer.writeModel(net, saveTemp, true);
        }
      }
      log.info("Saving model...");
      ModelSerializer.writeModel(net, locationToSave, true);
    }
    log.info("Done...");
  }

  private static ModelRating getModelRating(MultiLayerNetwork net, StockDataSetIterator iterator, INDArray max, INDArray min) {
    predicts = new INDArray[iterator.getTestDataSet().size()];
    actuals = new INDArray[iterator.getTestDataSet().size()];

    List<Bar> bars = new ArrayList();

    double totalRange = 0;
    double adjRange = 0;
    double overlapRange = 0;
    double overlapTotal = 0;
    int overlapTotalCount = 0;
    double adjOpen;
    double adjClose;
    double actOpen;
    double actClose;

    for (int i = 0; i < iterator.getTestDataSet().size(); i++) {
      predicts[i] = net.rnnTimeStep(iterator.getTestDataSet().get(i).getKey()).getRow(exampleLength - 1).mul(max.sub(min)).add(min);
      actuals[i] = iterator.getTestDataSet().get(i).getValue();
      net.rnnClearPreviousState();

      double offset = actuals[i].getDouble(0) - predicts[i].getDouble(0);

      adjOpen = actuals[i].getDouble(0);
      adjClose = predicts[i].getDouble(1) + offset;
      actOpen = actuals[i].getDouble(0);
      actClose = actuals[i].getDouble(1);
      bars.add(new BaseBar(
              Duration.ofDays(1),
              ZonedDateTime.ofLocal(iterator.getTestData().get(i).getDate(), ZoneId.systemDefault(), ZoneOffset.UTC),
              adjOpen,
              Math.max(adjOpen, adjClose),
              Math.min(adjOpen, adjClose),
              adjClose,
              0
      ));

      overlapRange = Math.max(adjOpen, adjClose) > Math.min(actOpen, actClose) && Math.min(adjOpen, adjClose) < Math.max(actOpen, actClose) ?
              Math.min(Math.max(adjOpen, adjClose), Math.max(actOpen, actClose)) -
                      Math.max(Math.min(actOpen, actClose), Math.min(adjOpen, adjClose)) : 0;
      if (overlapRange > 0) {
        overlapTotalCount++;
      }
      overlapTotal += overlapRange;
      adjRange += Math.abs(adjOpen - adjClose);
      totalRange += Math.abs(actOpen - actClose);
    }
    BaseBarSeriesBuilder barSeriesBuilder = new BaseBarSeriesBuilder();

    predictSeries = barSeriesBuilder.withName("PREDICT")
            .withBars(bars)
            .build();

    double overlapAverage = Math.floor((overlapTotal / totalRange) * (overlapTotal / adjRange) * 100);
    ModelRating modelRating = new ModelRating();
    modelRating.setOverlapPercent(((double) overlapTotalCount / iterator.getTestData().size()) * 100);
    modelRating.setAverageAdjusted(overlapAverage * ((double) overlapTotalCount / iterator.getTestData().size()));
    System.out.println("Overlap average: " + overlapAverage);
    System.out.println("Overlap overlapTotalCount: " + overlapTotalCount);
    System.out.println("Overlap overlapPercent: " + modelRating.getOverlapPercent());
    System.out.println("Overlap averageAdjusted: " + modelRating.getAverageAdjusted());
    return modelRating;
  }

  /**
   * Predict all the features (open, close, low, high prices and volume) of a stock one-day ahead
   */
  private static void plot(StockDataSetIterator iterator, String ticker, TradingRecord tradingRecord) {
    log.info("Print out Predictions and Actual Values...");
    log.info("Predict\tActual");
    for (int i = 0; i < predicts.length; i++) log.info(predicts[i] + "\t" + actuals[i]);
    log.info("Plot...");
    PlotUtil.plot(predicts, actuals, iterator, ticker, tradingRecord);
  }

}
