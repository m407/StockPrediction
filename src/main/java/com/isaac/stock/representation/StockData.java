package com.isaac.stock.representation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Created by zhanghao on 26/7/17.
 *
 * @author ZHANG HAO
 */
public class StockData {
  private String ticker; // stock name
  private String per; // stock period D/60/10
  private LocalDateTime date; // date
  private double[] data;


  public StockData(String ticker, String per, LocalDate date, LocalTime time, double[] data) {
    this.ticker = ticker;
    this.per = per;
    this.date = LocalDateTime.of(date, time);

    this.data = data;
  }


  public double[] getData() {
    return data;
  }

  public LocalDateTime getDate() {
    return date;
  }
}
