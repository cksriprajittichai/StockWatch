package c.chasesriprajittichai.stockwatch.stocks;

import java.util.ArrayList;

public final class PremarketStock extends AdvancedStock {

    private double mclose_price;
    private double mclose_changePoint;
    private double mclose_changePercent;

    public PremarketStock(final State state, final String ticker, final String name,
                          final double price, final double changePoint, final double changePercent,
                          final double close_price, final double close_changePoint,
                          final double close_changePercent, final double openPrice,
                          final double dayRangeLow, final double dayRangeHigh,
                          final double fiftyTwoWeekRangeLow, final double fiftyTwoWeekRangeHigh,
                          final String marketCap, final double beta, final double peRatio,
                          final double eps, final double yield, final String averageVolume,
                          final String description, final ArrayList<Double> yData_1day,
                          final ArrayList<Double> yData_2weeks, final ArrayList<Double> yData_1month,
                          final ArrayList<Double> yData_3months, final ArrayList<Double> yData_1year,
                          final ArrayList<Double> yData_5years) {
        super(state, ticker, name, price, changePoint, changePercent, openPrice, dayRangeLow,
                dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh, marketCap, beta, peRatio,
                eps, yield, averageVolume, description, yData_1day, yData_2weeks, yData_1month,
                yData_3months, yData_1year, yData_5years);
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
