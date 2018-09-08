package c.chasesriprajittichai.stockwatch.stocks;


/**
 * Should be implemented by a Stock that has a {@link Stock.State} equal to
 * {@link State#PREMARKET} or {@link State#AFTER_HOURS}. Stocks that implement
 * this have values from the last close and values from an extra hours trading
 * period. If the Stock's State is PREMARKET, the extra hours values are the
 * price, change point, and change percent that have occurred during PREMARKET
 * trading. If the Stock's State is AFTER_HOURS, the extra hours values are the
 * price, change point, and change percent that have occurred during AFTER_HOURS
 * trading. The price, change point, and change percent at the last close can be
 * accessed through their inherited getters: {@link Stock#getPrice()}, {@link
 * Stock#getChangePoint()}, and {@link Stock#getChangePercent()}.
 */
public interface StockWithEhVals extends Stock {

    /**
     * @return The price in extra hours trading. This is the live price.
     */
    double getExtraHoursPrice();

    void setExtraHoursPrice(final double extraHoursPrice);

    /**
     * @return The change point that has occurred during extra hours trading
     */
    double getExtraHoursChangePoint();

    void setExtraHoursChangePoint(final double extraHoursChangePoint);

    /**
     * @return The change percent that has occurred during extra hours trading
     */
    double getExtraHoursChangePercent();

    void setExtraHoursChangePercent(final double extraHoursChangePercent);

}
