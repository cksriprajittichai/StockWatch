package c.chasesriprajittichai.stockwatch.stocks;

public interface StockInHomeActivity extends Stock {

    // The sum of the change point during the open hours and the after hours
    double getNetChangePoint();

    // The sum of the change percent during the open hours and the after hours
    double getNetChangePercent();

    String[] getDataAsArray();

}
