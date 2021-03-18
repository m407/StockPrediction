package com.isaac.stock.representation;

/**
 * Created by zhanghao on 26/7/17.
 * @author ZHANG HAO
 */
public class StockData {
    private String ticker; // stock name
    private String per; // stock period D/60/10
    private String date; // date
    private String time; // time

    private double open; // open price
    private double close; // close price
    private double low; // low price
    private double high; // high price
    private double volume; // volume

    public StockData () {}

    public StockData (String ticker, String per, String date,  String time, double open, double close, double low, double high, double volume) {
        this.ticker = date;
        this.per = per;
        this.date = date;
        this.time  = time;

        this.open = open;
        this.close = close;
        this.low = low;
        this.high = high;
        this.volume = volume;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getticker() { return ticker; }
    public void setticker(String ticker) { this.ticker = ticker; }

    public double getOpen() { return open; }
    public void setOpen(double open) { this.open = open; }

    public double getClose() { return close; }
    public void setClose(double close) { this.close = close; }

    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }

    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }

    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
}
