package com.isaac.stock.representation;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.isaac.stock.representation.StockDataSetIterator.VECTOR_SIZE;

/**
 * Created by zhanghao on 26/7/17.
 *
 * @author ZHANG HAO
 */
public class StockData {
  private String ticker; // stock name
  private String per; // stock period D/60/10
  private LocalDateTime date; // date
  private Time time; // time


  public StockData(String ticker, String per, LocalDate date, LocalTime time, double[] data) {
    this.ticker = ticker;
    this.per = per;
    this.date = LocalDateTime.of(date, time);

    this.data = data;
  }


  public double[] getData() {
    return data;
  }

  private double[] data = new double[VECTOR_SIZE];

  public StockData() {
  }

  public LocalDateTime getDate() {
    return date;
  }
}
