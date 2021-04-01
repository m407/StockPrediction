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
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by zhanghao on 26/7/17.
 * Modified by zhanghao on 28/9/17.
 * @author ZHANG HAO
 */
public class StockPricePrediction {

    private static final Logger log = LoggerFactory.getLogger(StockPricePrediction.class);

    private static int exampleLength = 66; // time series length, assume 66 working days in 3 months

    public static void main (String[] args) throws IOException {
        String connectionString = System.getProperty("prices.connection", "jdbc:postgresql://localhost:5432/stock_prices");
        String ticker = System.getProperty("prices.ticker", "RI.RTSI");
        int batchSize = Integer.parseInt(System.getProperty("batchSize", "92")); // mini-batch size
        double splitRatio = 0.85; // 85% for training, 15% for testing
        int epochs = Integer.parseInt(System.getProperty("epochs", "1024"));; // training epochs

        log.info("Create dataSet iterator...");
        StockDataSetIterator iterator = new StockDataSetIterator(connectionString, ticker, batchSize, exampleLength, splitRatio);
        log.info("Load test dataset...");
        List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();

        log.info("Build lstm networks...");
        MultiLayerNetwork net = RecurrentNets.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes());

        String multiLayerNetworkFileName = System.getProperty("network.file", "StockPriceLSTM_" + ticker);
        File locationToSave = new File(multiLayerNetworkFileName+".zip");

        if(locationToSave.isFile() && locationToSave.exists()) {
            log.info("Load model...");
            net = ModelSerializer.restoreMultiLayerNetwork(locationToSave);
            log.info("Testing...");
                INDArray max = Nd4j.create(iterator.getMaxLabelArray());
                INDArray min = Nd4j.create(iterator.getMinLabeleArray());
                predictAllCategories(net, test, max, min);
        } else {
            // saveUpdater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this to train your network more in the future
            log.info("Training...");
            for (int i = 0; i < epochs; i++) {
                while (iterator.hasNext()) net.fit(iterator.next()); // fit model using mini-batch data
                iterator.reset(); // reset iterator
                net.rnnClearPreviousState(); // clear previous state
            }
            log.info("Saving model...");
            ModelSerializer.writeModel(net, locationToSave, true);
        }
        log.info("Done...");
    }

    /** Predict all the features (open, close, low, high prices and volume) of a stock one-day ahead */
    private static void predictAllCategories (MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, INDArray max, INDArray min) {
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
        for (int n = 0; n < 4; n++) {
            double[] pred = new double[predicts.length];
            double[] actu = new double[actuals.length];
            for (int i = 0; i < predicts.length; i++) {
                pred[i] = predicts[i].getDouble(n);
                actu[i] = actuals[i].getDouble(n);
            }
            String name;
            switch (n) {
                case 0: name = "Stock OPEN Price"; break;
                case 1: name = "Stock CLOSE Price"; break;
                case 2: name = "Stock LOW Price"; break;
                case 3: name = "Stock HIGH Price"; break;
                default: throw new NoSuchElementException();
            }
            PlotUtil.plot(pred, actu, name);
        }
    }

}
