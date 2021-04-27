package com.isaac.stock.representation;

import com.isaac.stock.predict.StockPricePrediction;
import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Created by zhanghao on 26/7/17.
 * Modified by zhanghao on 28/9/17.
 *
 * @author ZHANG HAO
 */
public class StockDataSetIterator implements DataSetIterator {

  public static int VECTOR_SIZE; // number of features for a stock data
  public static final int OUT_VECTOR_SIZE = 4; // number of features to predict
  private int miniBatchSize; // mini-batch size
  private int exampleLength; // default 22, say, 22 working days per month
  private int predictLength = 1; // default 1, say, one day ahead prediction
  private StockDataReader stockDataReader;
  /**
   * minimal values of each feature in stock dataset
   */
  private double[] minArray;
  /**
   * maximal values of each feature in stock dataset
   */
  private double[] maxArray;
  private int split;

  /**
   * mini-batch offset
   */
  private LinkedList<Integer> exampleStartOffsets = new LinkedList<>();

  /**
   * stock dataset for training
   */
  private List<StockData> trainData;
  /**
   * stock dataset for testing
   */
  private List<StockData> testData;

  public StockDataSetIterator(String ticker, int miniBatchSize, double splitRatio) {
    stockDataReader = new StockDataReader(ticker);
    List<StockData> stockDataList = stockDataReader.readAll();
    StockDataSetIterator.VECTOR_SIZE = stockDataReader.getVectorSize();
    this.maxArray = new double[stockDataReader.getVectorSize()];
    this.minArray = new double[stockDataReader.getVectorSize()];
    this.miniBatchSize = miniBatchSize;
    this.exampleLength = StockPricePrediction.exampleLength;
    this.split = (int) Math.round(stockDataList.size() * splitRatio);
    trainData = stockDataList.subList(0, split);
    testData = stockDataList.subList(split, stockDataList.size());
    test = generateTestDataSet(testData);
    initializeOffsets();
  }

  public List<StockData> getTestData() {
    return testData;
  }

  public LocalDateTime getTestFirstDay() {
    return testData.get(exampleLength).getDate();
  }

  public LocalDateTime getTestLastDay() {
    return testData.get(testData.size() - 1).getDate();
  }


  /**
   * adjusted stock dataset for testing
   */
  private List<Pair<INDArray, INDArray>> test;

  public StockDataReader getStockDataReader() {
    return stockDataReader;
  }

  /**
   * initialize the mini-batch offsets
   */
  private void initializeOffsets() {
    exampleStartOffsets.clear();
    int window = exampleLength + predictLength;
    for (int i = 0; i < trainData.size() - window; i++) {
      exampleStartOffsets.add(i);
    }
  }

  public List<Pair<INDArray, INDArray>> getTestDataSet() {
    return test;
  }

  public double[] getMaxLabelArray() {
    return Arrays.copyOfRange(maxArray, 0, OUT_VECTOR_SIZE);
  }

  public double[] getMinLabelArray() {
    return Arrays.copyOfRange(minArray, 0, OUT_VECTOR_SIZE);
  }

  public double[] getMaxArray() {
    return maxArray;
  }

  public double[] getMinArray() {
    return minArray;
  }

  @Override
  public DataSet next(int num) {
    if (exampleStartOffsets.size() == 0) throw new NoSuchElementException();
    int actualMiniBatchSize = Math.min(num, exampleStartOffsets.size());
    INDArray input = Nd4j.create(new int[]{actualMiniBatchSize, VECTOR_SIZE, exampleLength}, 'f');
    INDArray label = Nd4j.create(new int[]{actualMiniBatchSize, OUT_VECTOR_SIZE, exampleLength}, 'f');

    for (int index = 0; index < actualMiniBatchSize; index++) {
      int startIdx = exampleStartOffsets.removeFirst();
      int endIdx = startIdx + exampleLength;
      StockData curData = trainData.get(startIdx);
      StockData nextData;
      for (int i = startIdx; i < endIdx; i++) {
        int c = i - startIdx;
        for (int e = 0; e < VECTOR_SIZE; e++) {
          input.putScalar(new int[]{index, e, c}, (curData.getData()[e] - minArray[e]) / (maxArray[e] - minArray[e]));
        }
        nextData = trainData.get(i + 1);

        for (int e = 0; e < OUT_VECTOR_SIZE; e++) {
          label.putScalar(new int[]{index, e, c}, (nextData.getData()[e] - minArray[e]) / (maxArray[e] - minArray[e]));
        }

        curData = nextData;
      }
      if (exampleStartOffsets.size() == 0) break;
    }
    return new DataSet(input, label);
  }

  private List<Pair<INDArray, INDArray>> generateTestDataSet(List<StockData> stockDataList) {
    genMaxMinArrays();
    int window = exampleLength + predictLength;
    List<Pair<INDArray, INDArray>> test = new ArrayList<>();
    for (int index = 0; index < stockDataList.size() - window; index++) {
      INDArray input = Nd4j.create(new int[]{exampleLength, VECTOR_SIZE}, 'f');
      INDArray label = Nd4j.create(new int[]{OUT_VECTOR_SIZE}, 'f'); // ordering is set as 'f', faster construct

      for (int j = index; j < index + exampleLength; j++) {
        StockData stock = stockDataList.get(j);
        for (int e = 0; e < VECTOR_SIZE; e++) {
          input.putScalar(new int[]{j - index, e}, (stock.getData()[e] - minArray[e]) / (maxArray[e] - minArray[e]));
        }
      }
      StockData stock = stockDataList.get(index + exampleLength);
      for (int e = 0; e < OUT_VECTOR_SIZE; e++) {
        label.putScalar(new int[]{e}, stock.getData()[e]);
      }

      test.add(new Pair<>(input, label));
    }
    return test;
  }


  @Override
  public int totalExamples() {
    return trainData.size() - exampleLength - predictLength;
  }

  @Override
  public int inputColumns() {
    return VECTOR_SIZE;
  }

  @Override
  public int totalOutcomes() {
    return OUT_VECTOR_SIZE;
  }

  @Override
  public boolean resetSupported() {
    return false;
  }

  @Override
  public boolean asyncSupported() {
    return false;
  }

  @Override
  public void reset() {
    initializeOffsets();
  }

  @Override
  public int batch() {
    return miniBatchSize;
  }

  @Override
  public int cursor() {
    return totalExamples() - exampleStartOffsets.size();
  }

  @Override
  public int numExamples() {
    return totalExamples();
  }

  @Override
  public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  @Override
  public DataSetPreProcessor getPreProcessor() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  @Override
  public List<String> getLabels() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  @Override
  public boolean hasNext() {
    return exampleStartOffsets.size() > 0;
  }

  @Override
  public DataSet next() {
    return next(miniBatchSize);
  }

  private void genMaxMinArrays() {
    for (int i = 0; i < maxArray.length; i++) { // initialize max and min arrays
      maxArray[i] = Double.MIN_VALUE;
      minArray[i] = Double.MAX_VALUE;
    }

    trainData.forEach(stockData -> {
      for (int i = 0; i < VECTOR_SIZE - 4; i++) {
        if (stockData.getData()[i] > maxArray[i]) maxArray[i] = stockData.getData()[i];
        if (stockData.getData()[i] < minArray[i]) minArray[i] = stockData.getData()[i];
      }
    });
  }
}
