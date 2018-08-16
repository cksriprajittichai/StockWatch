package c.chasesriprajittichai.stockwatch.stocks;

import java.util.List;


public interface AdvancedStock extends Stock {

    enum ChartPeriod {
        ONE_DAY, TWO_WEEKS, ONE_MONTH, THREE_MONTHS, ONE_YEAR, FIVE_YEARS
    }

    double getPriceAtOpen();

    double getTodaysLow();

    double getTodaysHigh();

    double getFiftyTwoWeekLow();

    double getFiftyTwoWeekHigh();

    String getMarketCap();

    double getPrevClose();

    double getPeRatio();

    double getEps();

    double getYield();

    String getAverageVolume();

    String getDescription();

    List<Double> getPrices(final ChartPeriod chartPeriod);

    List<String> getDates(final ChartPeriod chartPeriod);

    List<Double> getPrices_1day();

    List<Double> getPrices_2weeks();

    List<Double> getPrices_1month();

    List<Double> getPrices_3months();

    List<Double> getPrices_1year();

    List<Double> getPrices_5years();

    List<String> getDates_2weeks();

    List<String> getDates_1month();

    List<String> getDates_3months();

    List<String> getDates_1year();

    List<String> getDates_5years();

}
