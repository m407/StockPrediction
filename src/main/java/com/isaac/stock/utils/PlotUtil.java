package com.isaac.stock.utils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Created by zhanghao on 26/7/17.
 * Modified by zhanghao on 28/9/17.
 *
 * @author ZHANG HAO
 */
public class PlotUtil {
  /**
   * Builds a JFreeChart time series from a Ta4j bar series and an indicator.
   *
   * @param barSeries the ta4j bar series
   * @param indicator the indicator
   * @param name      the name of the chart time series
   * @return the JFreeChart time series
   */
  private static org.jfree.data.time.TimeSeries buildChartBarSeries(BarSeries barSeries, Indicator<Num> indicator,
                                                                    String name) {
    org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
    for (int i = 0; i < barSeries.getBarCount(); i++) {
      Bar bar = barSeries.getBar(i);
      chartTimeSeries.add(new Day(Date.from(bar.getEndTime().toInstant())), indicator.getValue(i).doubleValue());
    }
    return chartTimeSeries;
  }

  /**
   * Displays a chart in a frame.
   *
   * @param chart the chart to be displayed
   */
  private static void displayChart(JFreeChart chart) {
    // Chart panel
    ChartPanel panel = new ChartPanel(chart);
    panel.setFillZoomRectangle(true);
    panel.setMouseWheelEnabled(true);
    panel.setPreferredSize(new java.awt.Dimension(500, 270));
    // Application frame
    ApplicationFrame frame = new ApplicationFrame("Stock chart");
    frame.setContentPane(panel);
    frame.pack();
    frame.setVisible(true);
  }

  public static void plot(INDArray[] predicts, INDArray[] actuals, String name, LocalDateTime startDate) {
    double[] index = new double[predicts.length];
    for (int i = 0; i < predicts.length; i++)
      index[i] = i;
    /*
     * Getting bar series
     */
    OHLCSeries predictsSeries = new OHLCSeries(name + "_predicts");
    OHLCSeries adjustedSeries = new OHLCSeries(name + "_p_adjusted");
    OHLCSeries actualsSeries = new OHLCSeries(name + "_actuals");
    double totalRange = 0;
    double adjRange = 0;
    double overlapRange = 0;
    for (int i = 0; i < predicts.length; i++) {
      predictsSeries.add(
              new Hour(0, startDate.plusDays(i).getDayOfMonth(), startDate.plusDays(i).getMonthValue(), startDate.plusDays(i).getYear()),
              predicts[i].getDouble(0),
              predicts[i].getDouble(1),
              predicts[i].getDouble(2),
              predicts[i].getDouble(3)
      );
      double adjOpen = actuals[i].getDouble(0);
      double adjHigh = predicts[i].getDouble(1) + actuals[i].getDouble(0) - predicts[i].getDouble(0);
      double adjLow = predicts[i].getDouble(2) + actuals[i].getDouble(0) - predicts[i].getDouble(0);
      double adjClose = predicts[i].getDouble(3) + actuals[i].getDouble(0) - predicts[i].getDouble(0);
      adjustedSeries.add(
              new Hour(3, startDate.plusDays(i).getDayOfMonth(), startDate.plusDays(i).getMonthValue(), startDate.plusDays(i).getYear()),
              adjOpen,
              adjHigh,
              adjLow,
              adjClose
      );

      double actOpen = actuals[i].getDouble(0);
      double actHigh = actuals[i].getDouble(1);
      double actLow = actuals[i].getDouble(2);
      double actClose = actuals[i].getDouble(3);
      actualsSeries.add(
              new Hour(6, startDate.plusDays(i).getDayOfMonth(), startDate.plusDays(i).getMonthValue(), startDate.plusDays(i).getYear()),
              actOpen,
              actHigh,
              actLow,
              actClose
      );

      overlapRange += Math.max(adjOpen, adjLow) > Math.min(actOpen, actClose) && Math.min(adjOpen, adjLow) < Math.max(actOpen, actClose) ?
              Math.min(Math.max(adjOpen, adjLow), Math.max(actOpen, actClose)) -
                      Math.max(Math.min(actOpen, actClose), Math.min(adjOpen, adjLow)) : 0;
      adjRange += Math.abs(adjOpen - adjLow);
      totalRange += Math.abs(actOpen - actClose);
    }

    System.out.println("Overlap average:" + (overlapRange / totalRange) * (1 - (adjRange - overlapRange) / totalRange));

    OHLCSeriesCollection dataset = new OHLCSeriesCollection();
    dataset.addSeries(predictsSeries);
    dataset.addSeries(adjustedSeries);
    dataset.addSeries(actualsSeries);

    /*
     * Creating the chart
     */
    JFreeChart jfreechart = ChartFactory.createCandlestickChart(name, "Time", "Value", dataset, true);
    XYPlot plot = (XYPlot) jfreechart.getPlot();
    DateAxis axis = (DateAxis) plot.getDomainAxis();
    XYItemRenderer renderer = plot.getRenderer();
    axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));

    /*
     * Displaying the chart
     */
    displayChart(jfreechart);
  }
}
