package com.isaac.stock.representation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockDataReader {
  private static final String user = "andrei";
  private static final String pass = "";
  private String connectionString = System.getProperty("prices.connection", "jdbc:postgresql://localhost:5432/stock_prices");
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
      connection = DriverManager.getConnection(connectionString, "andrei", "");
      System.out.println("Successfully Connected.");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  public List<StockData> readAll() {
    List<StockData> stockDataList = new ArrayList<>();
    try {
      Statement statement = connection.createStatement();

      ResultSet rs = statement.executeQuery("SELECT * FROM \"" + ticker + "\";");
      while (rs.next()) {
        vectorSize = rs.getMetaData().getColumnCount() - 4; // skip meta columns: date time per tiker
        double[] nums = new double[vectorSize];
        for (int i = 0; i < vectorSize - 4; i++) {
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
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
    }
    return stockDataList;
  }

  public StockData readOne(LocalDateTime localDateTime, String period) throws Exception {
    StockData stockData;
    Statement statement = connection.createStatement();

    ResultSet rs = statement.executeQuery("SELECT * FROM \"" + ticker + "\"" +
            " WHERE ticker='" + ticker + "' AND per='" + period + "'" +
            " AND date='" + localDateTime.toLocalDate() + "'::DATE;");
    return getSingleStockData(statement, rs);
  }

  public StockData readOneClosest(LocalDateTime localDateTime, String period) throws Exception {
    StockData stockData;
    Statement statement = connection.createStatement();

    ResultSet rs = statement.executeQuery("SELECT * FROM \"" + ticker + "\"" +
            " WHERE ticker='" + ticker + "' AND per='" + period + "'" +
            " AND date<='" + localDateTime.toLocalDate() + "'::DATE" +
            " LIMIT 1;");
    return getSingleStockData(statement, rs);
  }

  private StockData getSingleStockData(Statement statement, ResultSet rs) throws Exception {
    StockData stockData;
    if (rs.next()) {
      vectorSize = rs.getMetaData().getColumnCount() - 4; // skip meta columns: date time per tiker
      double[] nums = new double[vectorSize];
      for (int i = 0; i < vectorSize - 4; i++) {
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
