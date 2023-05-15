package com.isaac.stock.model;

import com.isaac.stock.predict.StockPricePrediction;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Created by zhanghao on 26/7/17.
 *
 * @author ZHANG HAO
 */
public class RecurrentNets {

  private static final double learningRate = 0.025;
  private static final int iterations = Integer.parseInt(System.getProperty("iterations", "1"));
  private static final int seed = 12345;

  private static final int lstmLayer1Size = Integer.parseInt(System.getProperty("lstmLayer1Size", "256"));
  private static final double lstmLayerRatio = Double.parseDouble(System.getProperty("lstmLayerRatio", "2"));
  private static final int lstmLayer2Size = (int) Math.round(lstmLayer1Size * lstmLayerRatio);
  private static final int denseLayerSize = lstmLayer1Size;
  private static final double dropoutRatio = 0.1;
  private static final int truncatedBPTTLength = StockPricePrediction.exampleLength; // exampleLength

  public static MultiLayerNetwork buildLstmNetworks(int nIn, int nOut) {
    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .maxNumLineSearchIterations(iterations)
            .weightInit(WeightInit.XAVIER)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.XAVIER)
            .updater(Updater.RMSPROP.getIUpdaterWithDefaultConfig())
            .l2(1e-4)
            .list()
            .layer(0, new GravesLSTM.Builder()
                    .nIn(nIn)
                    .nOut(lstmLayer1Size)
                    .activation(Activation.TANH)
                    .gateActivationFunction(Activation.HARDSIGMOID)
                    .dropOut(dropoutRatio)
                    .build())
            .layer(1, new GravesLSTM.Builder()
                    .nIn(lstmLayer1Size)
                    .nOut(lstmLayer2Size)
                    .activation(Activation.TANH)
                    .gateActivationFunction(Activation.HARDSIGMOID)
                    .dropOut(dropoutRatio)
                    .build())
            .layer(2, new DenseLayer.Builder()
                    .nIn(lstmLayer2Size)
                    .nOut(denseLayerSize)
                    .activation(Activation.RELU)
                    .build())
            .layer(3, new RnnOutputLayer.Builder()
                    .nIn(denseLayerSize)
                    .nOut(nOut)
                    .activation(Activation.IDENTITY)
                    .lossFunction(LossFunctions.LossFunction.MSE)
                    .build())
            .backpropType(BackpropType.TruncatedBPTT)
            .tBPTTForwardLength(truncatedBPTTLength)
            .tBPTTBackwardLength(truncatedBPTTLength)
            .backpropType(BackpropType.Standard)
            .build();

    MultiLayerNetwork net = new MultiLayerNetwork(conf);
    net.init();
    net.setListeners(new ScoreIterationListener(100));
    return net;
  }
}
