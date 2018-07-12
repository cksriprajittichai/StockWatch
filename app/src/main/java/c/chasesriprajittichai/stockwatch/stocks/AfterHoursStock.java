package c.chasesriprajittichai.stockwatch.stocks;

import java.util.List;

public final class AfterHoursStock extends AdvancedStock implements StockWithCloseValues {

    private double mclose_price;
    private double mclose_changePoint;
    private double mclose_changePercent;

    public AfterHoursStock(final State state, final String ticker, final String name,
                           final double price, final double changePoint, final double changePercent,
                           final double close_price, final double close_changePoint,
                           final double close_changePercent, final double openPrice,
                           final double dayRangeLow, final double dayRangeHigh,
                           final double fiftyTwoWeekRangeLow, final double fiftyTwoWeekRangeHigh,
                           final String marketCap, final double beta, final double peRatio,
                           final double eps, final double yield, final String averageVolume,
                           final String description, final List<Double> yData_1day,
                           final List<Double> yData_2weeks, final List<Double> yData_1month,
                           final List<Double> yData_3months, final List<Double> yData_1year,
                           final List<Double> yData_5years, final List<String> dates_2weeks,
                           final List<String> dates_1month, final List<String> dates_3months,
                           final List<String> dates_1year, final List<String> dates_5years) {
        super(state, ticker, name, price, changePoint, changePercent, openPrice, dayRangeLow,
                dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh, marketCap, beta, peRatio,
                eps, yield, averageVolume, description, yData_1day, yData_2weeks, yData_1month,
                yData_3months, yData_1year, yData_5years, dates_2weeks, dates_1month, dates_3months,
                dates_1year, dates_5years);
        mclose_price = close_price;
        mclose_changePoint = close_changePoint;
        mclose_changePercent = close_changePercent;
    }

    public double getClose_price() {
        return mclose_price;
    }

    public double getClose_changePoint() {
        return mclose_changePoint;
    }

    public double getClose_changePercent() {
        return mclose_changePercent;
    }

}
