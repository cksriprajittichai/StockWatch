package com.sienga.stockwatch.stocks;

public interface Stock {

    /**
     * A Stock's State does not represent the current state of the market. The
     * primary purpose of a Stock's State is to be used to determine whether or
     * not a Stock is a {@link StockWithEhVals}.
     * <p>
     * The most common example of a Stock's State not representing the state of
     * the market is if you were to look at a Stock on a weekend day. If the
     * Stock participated in after hours trading on the last trading day, then
     * the Stock will have the day's open trading hours values, as well as that
     * day's after hours values. Because the Stock has after hours values, its
     * State will be AFTER_HOURS, but the market will be closed, because it is a
     * weekend day.
     */
    enum State {
        ERROR, PREMARKET, OPEN, AFTER_HOURS, CLOSED
    }

    /**
     * @return The most current price of the Stock
     */
    double getLivePrice();

    /**
     * @return If Stock instanceof {@link StockWithEhVals}, return the change
     * point from the extra hours trading. Otherwise, return the change point
     * from the open trading hours.
     */
    double getLiveChangePoint();

    /**
     * @return If Stock instanceof {@link StockWithEhVals}, return the change
     * percent from the extra hours trading. Otherwise, return the change
     * percent from the open trading hours.
     */
    double getLiveChangePercent();

    String getTicker();

    String getName();

    State getState();

    void setState(final State state);

    double getPrice();

    void setPrice(final double price);

    double getChangePoint();

    void setChangePoint(final double changePoint);

    double getChangePercent();

    void setChangePercent(final double changePercent);

}
