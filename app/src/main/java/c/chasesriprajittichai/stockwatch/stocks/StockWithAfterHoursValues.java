package c.chasesriprajittichai.stockwatch.stocks;


import java.util.List;


public abstract class StockWithAfterHoursValues extends AdvancedStock {
    /* Should be implemented by a stock that has a state that is not OPEN or
     * CLOSED, and has price, change point, and change percent values at the
     * the stock's last close.
     * The price at the last close can be accessed through getPrice(), which is
     * inherited from BasicStock. AfterHoursPrice tracks the price in after
     * hours, which is the live price. This same convention is used for change
     * point and change percent. */

    // Price in after hours; live price
    private double afterHoursPrice;

    // The change point that has occurred during after hours
    private double afterHoursChangePoint;

    // The change percent that has occurred during after hours
    private double afterHoursChangePercent;

    StockWithAfterHoursValues(final State state, final String ticker, final String name,
                              final double priceAtClose, final double changePointAtClose,
                              final double changePercentAtClose, final double afterHoursPrice,
                              final double afterHoursChangePoint, final double afterHoursChangePercent,
                              final double todaysLow, final double todaysHigh, final double fiftyTwoWeekLow,
                              final double fiftyTwoWeekHigh, final String marketCap,
                              final double prevClose, final double peRatio, final double eps,
                              final double yield, final String averageVolume, final String description,
                              final List<Double> prices_1day, final List<Double> prices_2weeks,
                              final List<Double> prices_1month, final List<Double> prices_3months,
                              final List<Double> prices_1year, final List<Double> prices_5years,
                              final List<String> dates_2weeks, final List<String> dates_1month,
                              final List<String> dates_3months, final List<String> dates_1year,
                              final List<String> dates_5years) {
        super(state, ticker, name, priceAtClose, changePointAtClose, changePercentAtClose, todaysLow,
                todaysHigh, fiftyTwoWeekLow, fiftyTwoWeekHigh, marketCap, prevClose, peRatio,
                eps, yield, averageVolume, description, prices_1day, prices_2weeks, prices_1month,
                prices_3months, prices_1year, prices_5years, dates_2weeks, dates_1month, dates_3months,
                dates_1year, dates_5years);
        this.afterHoursPrice = afterHoursPrice;
        this.afterHoursChangePoint = afterHoursChangePoint;
        this.afterHoursChangePercent = afterHoursChangePercent;
    }

    @Override
    public double getLivePrice() {
        return afterHoursPrice;
    }

    @Override
    public double getLiveChangePoint() {
        return afterHoursChangePoint;
    }

    @Override
    public double getLiveChangePercent() {
        return afterHoursChangePercent;
    }

}
