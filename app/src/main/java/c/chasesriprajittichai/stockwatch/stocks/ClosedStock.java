package c.chasesriprajittichai.stockwatch.stocks;

import java.util.List;

public final class ClosedStock extends AdvancedStock {

    public ClosedStock(String ticker, String name, double price, double changePoint,
                       double changePercent, double todaysLow, double todaysHigh,
                       double fiftyTwoWeekLow, double fiftyTwoWeekHigh, String marketCap,
                       double prevClose, double peRatio, double eps, double yield,
                       String averageVolume, String description, List<Double> yData_1day,
                       List<Double> yData_2weeks, List<Double> yData_1month,
                       List<Double> yData_3months, List<Double> yData_1year,
                       List<Double> yData_5years, List<String> dates_2weeks,
                       List<String> dates_1month, List<String> dates_3months,
                       List<String> dates_1year, List<String> dates_5years) {
        super(State.CLOSED, ticker, name, price, changePoint, changePercent, todaysLow, todaysHigh,
                fiftyTwoWeekLow, fiftyTwoWeekHigh, marketCap, prevClose, peRatio, eps, yield,
                averageVolume, description, yData_1day, yData_2weeks, yData_1month, yData_3months,
                yData_1year, yData_5years, dates_2weeks, dates_1month, dates_3months, dates_1year,
                dates_5years);
    }
}
