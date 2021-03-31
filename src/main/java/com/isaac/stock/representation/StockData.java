package com.isaac.stock.representation;

import java.sql.Time;
import java.sql.Date;

import static com.isaac.stock.representation.StockDataSetIterator.*;

/**
 * Created by zhanghao on 26/7/17.
 *
 * @author ZHANG HAO
 */
public class StockData {
  private String ticker; // stock name
  private String per; // stock period D/60/10
  private Date date; // date
  private Time time; // time


  public double[] getData() {
    return data;
  }

  private double[] data = new double[VECTOR_SIZE];

  public StockData() {
  }

  public StockData(String ticker, String per, Date date, Time time, double[] data) {
    this.ticker = ticker;
    this.per = per;
    this.date = date;
    this.time = time;

    this.data = data;
  }
}
