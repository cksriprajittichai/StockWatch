package c.chasesriprajittichai.stockwatch.stocks;

import java.util.List;


public final class AfterHoursStock extends StockWithAfterHoursValues {

    public AfterHoursStock(final String ticker, final String name, final double priceAtClose,
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
        super(State.AFTER_HOURS, ticker, name, priceAtClose, changePointAtClose,
                changePercentAtClose, afterHoursPrice, afterHoursChangePoint, afterHoursChangePercent,
                todaysLow, todaysHigh, fiftyTwoWeekLow, fiftyTwoWeekHigh, marketCap, prevClose,
                peRatio, eps, yield, averageVolume, description, prices_1day, prices_2weeks,
                prices_1month, prices_3months, prices_1year, prices_5years, dates_2weeks,
                dates_1month, dates_3months, dates_1year, dates_5years);
    }

}
