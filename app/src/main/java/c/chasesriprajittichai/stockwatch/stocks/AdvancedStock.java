package c.chasesriprajittichai.stockwatch.stocks;

import java.util.List;


public interface AdvancedStock extends Stock {

    enum ChartPeriod {
        ONE_DAY, TWO_WEEKS, ONE_MONTH, THREE_MONTHS, ONE_YEAR, FIVE_YEARS
    }

    enum Stat {
        TODAYS_RANGE, FIFTY_TWO_WEEK_RANGE, MARKET_CAP, PREV_CLOSE, PE_RATIO,
        EPS, YIELD, AVG_VOLUME, DESCRIPTION
    }

    double getPriceAtOpen();

    void setPriceAtOpen(final double priceAtOpen);

    double getTodaysLow();

    void setTodaysLow(final double todaysLow);

    double getTodaysHigh();

    void setTodaysHigh(final double todaysHigh);

    double getFiftyTwoWeekLow();

    void setFiftyTwoWeekLow(final double fiftyTwoWeekLow);

    double getFiftyTwoWeekHigh();

    void setFiftyTwoWeekHigh(final double fiftyTwoWeekHigh);

    String getMarketCap();

    void setMarketCap(final String marketCap);

    double getPrevClose();

    void setPrevClose(final double prevClose);

    double getPeRatio();

    void setPeRatio(final double peRatio);

    double getEps();

    void setEps(final double eps);

    double getYield();

    void setYield(final double yield);

    String getAverageVolume();

    void setAverageVolume(final String avgVolume);

    String getDescription();

    void setDescription(final String description);

    List<Double> getPrices(final ChartPeriod chartPeriod);

    List<String> getDates(final ChartPeriod chartPeriod);

    List<Double> getPrices_1day();

    void setPrices_1day(final List<Double> prices);

    List<Double> getPrices_2weeks();

    void setPrices_2weeks(final List<Double> prices);

    List<Double> getPrices_1month();

    void setPrices_1month(final List<Double> prices);

    List<Double> getPrices_3months();

    void setPrices_3months(final List<Double> prices);

    List<Double> getPrices_1year();

    void setPrices_1year(final List<Double> prices);

    List<Double> getPrices_5years();

    void setPrices_5years(final List<Double> prices);

    List<String> getDates_2weeks();

    void setDates_2weeks(final List<String> dates);

    List<String> getDates_1month();

    void setDates_1month(final List<String> dates);

    List<String> getDates_3months();

    void setDates_3months(final List<String> dates);

    List<String> getDates_1year();

    void setDates_1year(final List<String> dates);

    List<String> getDates_5years();

    void setDates_5years(final List<String> dates);

}
