package com.isaac.stock.predict;

import com.isaac.stock.model.RecurrentNets;
import com.isaac.stock.representation.StockDataSetIterator;
import com.isaac.stock.utils.PlotUtil;
import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by zhanghao on 26/7/17.
 * Modified by zhanghao on 28/9/17.
 *
 * @author ZHANG HAO
 */
public class StockPricePrediction {

  private static final Logger log = LoggerFactory.getLogger(StockPricePrediction.class);

  private static int exampleLength = 66; // time series length, assume 66 working days in 3 months

  public static void main(String[] args) throws IOException {
    String connectionString = System.getProperty("prices.connection", "jdbc:postgresql://localhost:5432/stock_prices");
    String ticker = System.getProperty("prices.ticker", "RI.RTSI");
    int iterations = Integer.parseInt(System.getProperty("iterations", "1"));
    int lstmLayer1Size = Integer.parseInt(System.getProperty("lstmLayer1Size", "256"));
    int batchSize = Integer.parseInt(System.getProperty("batchSize", "86")); // mini-batch size
    double splitRatio = 0.85; // 85% for training, 15% for testing
    int epochs = Integer.parseInt(System.getProperty("epochs", "8192"));
    ; // training epochs

    log.info("Create dataSet iterator...");
    StockDataSetIterator iterator = new StockDataSetIterator(connectionString, ticker, batchSize, exampleLength, splitRatio);
    log.info("Load test dataset...");
    List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();

    log.info("Build lstm networks...");
    MultiLayerNetwork net = RecurrentNets.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes());

    String defaultNetworkFileName = "StockPriceLSTM_" + ticker + ".b" + batchSize + ".i" + iterations + ".lstm" + lstmLayer1Size;
    String multiLayerNetworkFileName = System.getProperty("network.file", defaultNetworkFileName);
    multiLayerNetworkFileName = multiLayerNetworkFileName.replaceFirst("\\.zip", "");
    File locationToSave = new File(multiLayerNetworkFileName + ".zip");

    INDArray max = Nd4j.create(iterator.getMaxLabelArray());
    INDArray min = Nd4j.create(iterator.getMinLabeleArray());

    if (locationToSave.isFile() && locationToSave.exists()) {
      log.info("Load model...");
      net = ModelSerializer.restoreMultiLayerNetwork(locationToSave);
      log.info("Testing...");
      getModelRating(net, test, max, min);
      predictAllCategories(net, test, max, min, multiLayerNetworkFileName, iterator.getLastDate().plusDays(1));
    } else {
      // saveUpdater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this to train your network more in the future
      log.info("Training...");
      Double currentModelRating = 0.0;
      for (int i = 0; i < epochs; i++) {
        while (iterator.hasNext()) net.fit(iterator.next()); // fit model using mini-batch data
        iterator.reset(); // reset iterator
        net.rnnClearPreviousState(); // clear previous state
        if (i % 16 == 0) {
          Double modelRating = getModelRating(net, test, max, min);
          if (modelRating > 10 || modelRating > currentModelRating) {
            currentModelRating = modelRating;
            File saveTemp = new File(multiLayerNetworkFileName + ".rating" + modelRating + "." + i + ".zip");
            ModelSerializer.writeModel(net, saveTemp, true);
          }
        }
      }
      log.info("Saving model...");
      ModelSerializer.writeModel(net, locationToSave, true);
    }
    log.info("Done...");
  }

  private static Double getModelRating(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, INDArray max, INDArray min) {
    INDArray[] predicts = new INDArray[testData.size()];
    INDArray[] actuals = new INDArray[testData.size()];

    double totalRange = 0;
    double adjRange = 0;
    double overlapRange = 0;
    double adjOpen;
    double adjClose;
    double actOpen;
    double actClose;

    for (int i = 0; i < testData.size(); i++) {
      predicts[i] = net.rnnTimeStep(testData.get(i).getKey()).getRow(exampleLength - 1).mul(max.sub(min)).add(min);
      actuals[i] = testData.get(i).getValue();

      adjOpen = actuals[i].getDouble(0);
      adjClose = predicts[i].getDouble(2) + actuals[i].getDouble(0) - predicts[i].getDouble(0);
      actOpen = actuals[i].getDouble(0);
      actClose = actuals[i].getDouble(3);

      overlapRange += Math.max(adjOpen, adjClose) > Math.min(actOpen, actClose) && Math.min(adjOpen, adjClose) < Math.max(actOpen, actClose) ?
              Math.min(Math.max(adjOpen, adjClose), Math.max(actOpen, actClose)) -
                      Math.max(Math.min(actOpen, actClose), Math.min(adjOpen, adjClose)) : 0;
      adjRange += Math.abs(adjOpen - adjClose);
      totalRange += Math.abs(actOpen - actClose);
    }
    double overlapAverage = Math.floor((overlapRange / totalRange) * (overlapRange / adjRange) * 100);
    System.out.println("Overlap average: " + overlapAverage);
    return overlapAverage;
  }

  /**
   * Predict all the features (open, close, low, high prices and volume) of a stock one-day ahead
   */
  private static void predictAllCategories(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, INDArray max, INDArray min, String ticker, LocalDateTime startDate) {
    INDArray[] predicts = new INDArray[testData.size()];
    INDArray[] actuals = new INDArray[testData.size()];
    for (int i = 0; i < testData.size(); i++) {
      predicts[i] = net.rnnTimeStep(testData.get(i).getKey()).getRow(exampleLength - 1).mul(max.sub(min)).add(min);
      actuals[i] = testData.get(i).getValue();
    }
    log.info("Print out Predictions and Actual Values...");
    log.info("Predict\tActual");
    for (int i = 0; i < predicts.length; i++) log.info(predicts[i] + "\t" + actuals[i]);
    log.info("Plot...");
    PlotUtil.plot(predicts, actuals, ticker, startDate);
  }

}
