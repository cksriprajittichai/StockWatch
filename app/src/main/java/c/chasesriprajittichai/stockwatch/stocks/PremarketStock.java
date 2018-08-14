package c.chasesriprajittichai.stockwatch.stocks;

import java.util.List;


public final class PremarketStock extends AdvancedStock implements StockWithAfterHoursValues {
    /* The price at the last close can be accessed through this.getPrice(), which is
     * inherited from BasicStock. AfterHoursPrice tracks the price in after hours, which
     * is the live price. This same convention is used for change point and change percent. */

    // Price in after hours; live price
    private double afterHoursPrice;

    // The change point that has occurred during after hours
    private double afterHoursChangePoint;

    // The change percent that has occurred during after hours
    private double afterHoursChangePercent;

    public PremarketStock(final String ticker, final String name, final double priceAtClose,
                          final double changePointAtClose, final double changePercentAtClose,
                          final double afterHoursPrice, final double afterHoursChangePoint,
                          final double afterHoursChangePercent, final double todaysLow,
                          final double todaysHigh, final double fiftyTwoWeekLow,
                          final double fiftyTwoWeekHigh, final String marketCap,
                          final double prevClose, final double peRatio, final double eps,
                          final double yield, final String averageVolume,
                          final String description, final List<Double> prices_1day,
                          final List<Double> prices_2weeks, final List<Double> prices_1month,
                          final List<Double> prices_3months, final List<Double> prices_1year,
                          final List<Double> prices_5years, final List<String> dates_2weeks,
                          final List<String> dates_1month, final List<String> dates_3months,
                          final List<String> dates_1year, final List<String> dates_5years) {
        super(State.PREMARKET, ticker, name, priceAtClose, changePointAtClose, changePercentAtClose, todaysLow,
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
        return getAfterHoursPrice();
    }

    @Override
    public double getLiveChangePoint() {
        return getAfterHoursChangePoint();
    }

    @Override
    public double getLiveChangePercent() {
        return getAfterHoursChangePercent();
    }

    @Override
    public double getAfterHoursPrice() {
        return afterHoursPrice;
    }

    @Override
    public double getAfterHoursChangePoint() {
        return afterHoursChangePoint;
    }

    @Override
    public double getAfterHoursChangePercent() {
        return afterHoursChangePercent;
    }

}
