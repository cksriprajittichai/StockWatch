package c.chasesriprajittichai.stockwatch.stocks;


/**
 * Should be implemented by a Stock that has a state that is {@link
 * State#PREMARKET} or {@link State#AFTER_HOURS}, and has price, change point,
 * and change percent values at the Stock's last close. Stocks that implement
 * this have values at the last close (values from the open trading hours) and
 * values during after hours trading period. The price, change point, and change
 * percent at the last close can be accessed through their inherited getters:
 * {@link Stock#getPrice()}, {@link Stock#getChangePoint()}, and {@link
 * Stock#getChangePercent()}.
 */
public interface StockWithAhVals extends Stock {

    /**
     * @return The price in after hours trading. This is the live price.
     */
    double getAfterHoursPrice();

    void setAfterHoursPrice(final double afterHoursPrice);

    /**
     * @return The change point that has occurred during after hours trading
     */
    double getAfterHoursChangePoint();

    void setAfterHoursChangePoint(final double afterHoursChangePoint);

    /**
     * @return The change percent that has occurred during after hours trading
     */
    double getAfterHoursChangePercent();

    void setAfterHoursChangePercent(final double afterHoursChangePercent);

}
