package c.chasesriprajittichai.stockwatch.stocks;


public interface StockWithAhVals extends BasicStock {
    /* Should be implemented by a stock that has a state that is PREMARKET or
     * AFTER_HOURS, and has price, change point, and change percent values at
     * the stock's last close. Stocks that implement this have values at the
     * last close, as well as values during the after hours trading period.
     * The price at the last close can be accessed through getPrice(), which is
     * inherited from BasicStock. */

    @Override
    double getLivePrice();

    @Override
    double getLiveChangePoint();

    @Override
    double getLiveChangePercent();

    // The sum of the change point during the open hours and the after hours
    @Override
    double getNetChangePoint();

    // The sum of the change percent during the open hours and the after hours
    @Override
    double getNetChangePercent();

    // Price in after hours. This is the live price.
    double getAfterHoursPrice();

    void setAfterHoursPrice(final double afterHoursPrice);

    // The change point that has occurred during after hours
    double getAfterHoursChangePoint();

    void setAfterHoursChangePoint(final double afterHoursChangePoint);

    // The change percent that has occurred during after hours
    double getAfterHoursChangePercent();

    void setAfterHoursChangePercent(final double afterHoursChangePercent);

}
