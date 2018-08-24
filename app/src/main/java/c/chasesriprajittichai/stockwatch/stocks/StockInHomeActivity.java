package c.chasesriprajittichai.stockwatch.stocks;

public interface StockInHomeActivity extends Stock {

    /**
     * @return The sum of the change percent during the open trading hours and
     * after hours trading
     */
    double getNetChangePercent();

    /**
     * @return A four element string array containing the {@link
     * StockInHomeActivity}'s {@link Stock.State}, price, change point, and
     * change percent.
     */
    String[] getDataAsArray();

}
