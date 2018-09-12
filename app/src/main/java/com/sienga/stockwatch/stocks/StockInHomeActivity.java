package com.sienga.stockwatch.stocks;

public interface StockInHomeActivity extends Stock {

    /**
     * @return The sum of the change percent from the premarket hours, the open
     * hours, and the after hours of the live or most recent trading day.
     */
    double getNetChangePercent();

    /**
     * @return A string array containing the values of the {@link
     * StockInHomeActivity}.
     */
    String[] getDataAsArray();

}
