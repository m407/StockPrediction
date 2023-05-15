package com.isaac.stock.representation;

import com.isaac.stock.predict.StockPricePrediction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockDataReader {
  private static final String user = "stockdata";
  private static final String pass = "stockdata";
  private String connectionString = System.getProperty("prices.connection", "jdbc:postgresql://localhost:5432/stockdata");
  private String ticker;
  private Integer vectorSize;

  private Connection connection;

  public StockDataReader(String ticker) {
    this.ticker = ticker;
    this.connect();
  }

  public Integer getVectorSize() {
    return vectorSize;
  }

  private void connect() {
    try {
      Class.forName("org.postgresql.Driver");
      connection = DriverManager.getConnection(connectionString, "stockdata", "stockdata");
      System.out.println("Successfully Connected.");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  public List<StockData> readAll() {
    List<StockData> stockDataList = new ArrayList<>();
    try {
      String query = "SELECT * FROM \"" + ticker + "\";";
      stockDataList = getAllStockData(query);
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
    }
    return stockDataList;
  }

  public StockData readOne(LocalDateTime localDateTime, String period) throws Exception {
    String query = "SELECT * FROM \"" + ticker + "\"" +
            " WHERE ticker='" + ticker + "' AND per='" + period + "'" +
            " AND date='" + localDateTime.toLocalDate() + "'::DATE;";
    return getSingleStockData(query);
  }

  public List<StockData> readClosestExample(LocalDateTime localDateTime, String period) throws Exception {
    List<StockData> stockDataList = new ArrayList<>();
    try {
      Statement statement = connection.createStatement();

      String query = "SELECT * FROM \"" + ticker + "\"" +
              " WHERE ticker='" + ticker + "' AND per='" + period + "'" +
              " AND date<='" + localDateTime.toLocalDate() + "'::DATE" +
              " LIMIT " + StockPricePrediction.exampleLength + ";";
      stockDataList = getAllStockData(query);
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
    }
    return stockDataList;
  }

  private List<StockData> getAllStockData(String query) throws SQLException {
    Statement statement = connection.createStatement();
    ResultSet rs = statement.executeQuery(query);
    List<StockData> stockDataList = new ArrayList<>();
    while (rs.next()) {
      vectorSize = rs.getMetaData().getColumnCount() - 4; // skip meta columns: date time per tiker
      double[] nums = new double[vectorSize];
      for (int i = 0; i < vectorSize; i++) {
        nums[i] = rs.getDouble(i + 5);
      }
      stockDataList.add(new StockData(
              rs.getString(1),
              rs.getString(2),
              rs.getDate(3).toLocalDate(),
              rs.getTime(4).toLocalTime(),
              nums));
    }
    rs.close();
    statement.close();
    return stockDataList;
  }

  private StockData getSingleStockData(String query) throws Exception {
    Statement statement = connection.createStatement();
    ResultSet rs = statement.executeQuery(query);

    StockData stockData;
    if (rs.next()) {
      vectorSize = rs.getMetaData().getColumnCount() - 4; // skip meta columns: date time per tiker
      double[] nums = new double[vectorSize];
      for (int i = 0; i < vectorSize; i++) {
        nums[i] = rs.getDouble(i + 5);
      }
      stockData = new StockData(
              rs.getString(1),
              rs.getString(2),
              rs.getDate(3).toLocalDate(),
              rs.getTime(4).toLocalTime(),
              nums);
      rs.close();
      statement.close();
      return stockData;
    }
    throw new Exception("Empty data");
  }


}
