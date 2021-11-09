package com.isaac.stock.utils;

import com.isaac.stock.representation.StockDataReader;
import com.isaac.stock.representation.StockDataSetIterator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Minute;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.ta4j.core.*;
import org.ta4j.core.num.Num;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

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

  public static void plot(INDArray[] predicts, INDArray[] actuals, StockDataSetIterator iterator, String name, TradingRecord tradingRecord) {
    /*
     * Getting bar series
     */
    LocalDateTime startDate = iterator.getTestFirstDay();
    LocalDateTime endDate = iterator.getTestLastDay();
    StockDataReader stockDataReader = new StockDataReader("RI.RTSI.10");

    OHLCSeries predictsSeries = new OHLCSeries(name + "_predicts");
    OHLCSeries adjustedSeries = new OHLCSeries(name + "_p_adjusted");
    OHLCSeries actualsSeries = new OHLCSeries(name + "_actuals");
    OHLCSeries m10Series = new OHLCSeries(name + "_m10");
    stockDataReader
            .readAll()
            .stream()
            .filter(item -> (item.getDate().isAfter(startDate) ||
                    item.getDate().isEqual(startDate)) &&
                    (item.getDate().isBefore(endDate) ||
                            item.getDate().isEqual(endDate))
            )
            .forEachOrdered(item -> m10Series.add(
                    new Minute(Date.from(item.getDate().toInstant(ZoneOffset.UTC))),
                    item.getData()[0],
                    item.getData()[1],
                    item.getData()[2],
                    item.getData()[3]
            ));

    for (int i = 0; i < predicts.length; i++) {
      LocalDateTime stockDate = iterator.getTestData().get(i).getDate();
      double predictsOpen = predicts[i].getDouble(0);
      double predictsClose = predicts[i].getDouble(1);
      double predictsHigh = predictsOpen > predictsClose ? predictsOpen : predictsClose;
      double predictsLow = predictsOpen > predictsClose ? predictsClose : predictsOpen;

      predictsSeries.add(
              new Hour(Date.from(stockDate.plusHours(-2).toInstant(ZoneOffset.UTC))),
              predictsOpen,
              predictsHigh,
              predictsLow,
              predictsClose
      );
      double adjOpen = actuals[i].getDouble(0);
      double adjHigh = predictsHigh + adjOpen - predictsOpen;
      double adjLow = predictsLow + adjOpen - predictsOpen;
      double adjClose = predictsClose + adjOpen - predictsOpen;
      adjustedSeries.add(
              new Hour(Date.from(stockDate.toInstant(ZoneOffset.UTC))),
              adjOpen,
              adjHigh,
              adjLow,
              adjClose
      );

      double actOpen = actuals[i].getDouble(0);
      double actClose = actuals[i].getDouble(1);
      double actHigh = actOpen > actClose ? actOpen : actClose;
      double actLow = actOpen > actClose ? actClose : actOpen;
      actualsSeries.add(
              new Hour(Date.from(stockDate.plusHours(2).toInstant(ZoneOffset.UTC))),
              actOpen,
              actHigh,
              actLow,
              actClose
      );
    }

    OHLCSeriesCollection dataset = new OHLCSeriesCollection();
    dataset.addSeries(m10Series);
    dataset.addSeries(predictsSeries);
    dataset.addSeries(adjustedSeries);
    dataset.addSeries(actualsSeries);

    /*
     * Creating the chart
     */
    JFreeChart jfreechart = ChartFactory.createCandlestickChart(name, "Time", "Value", dataset, true);
    XYPlot plot = (XYPlot) jfreechart.getPlot();
    DateAxis axis = (DateAxis) plot.getDomainAxis();
    axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));

    List<Position> positionList = tradingRecord.getPositions();
    // Adding markers to plot
    for (Position position : positionList) {
      // Buy signal
      double buySignalTickTime = new Minute(Date.from(position.getEntry().getIndex()series.getTick(trade.getEntry().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
      Marker buyMarker = new ValueMarker(buySignalTickTime);
      buyMarker.setPaint(Color.GREEN);
      buyMarker.setLabel("B");
      plot.addDomainMarker(buyMarker);
      // Sell signal
      double sellSignalTickTime = new Minute(Date.from(series.getTick(trade.getExit().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
      Marker sellMarker = new ValueMarker(sellSignalTickTime);
      sellMarker.setPaint(Color.RED);
      sellMarker.setLabel("S");
      plot.addDomainMarker(sellMarker);
    }


    /*
     * Displaying the chart
     */
    displayChart(jfreechart);
  }
}
