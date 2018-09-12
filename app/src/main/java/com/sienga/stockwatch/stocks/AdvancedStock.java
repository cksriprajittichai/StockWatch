package com.sienga.stockwatch.stocks;

import java.util.List;
import java.util.Set;

import com.sienga.stockwatch.CustomSparkView;
import com.sienga.stockwatch.IndividualStockActivity;


public interface AdvancedStock extends Stock {

    /**
     * These are the different time periods that can be shown in the {@link
     * CustomSparkView} in {@link IndividualStockActivity}. This enum is used
     * very often to specify a Stock's chart.
     */
    enum ChartPeriod {
        ONE_DAY, TWO_WEEKS, ONE_MONTH, THREE_MONTHS, ONE_YEAR, FIVE_YEARS
    }

    /**
     * These represent the member variables of an AdvancedStock, excluding the
     * chart data (prices and dates). This enum is used in {@link
     * IndividualStockActivity.DownloadStatsTask} and {@link
     * IndividualStockActivity#onDownloadStatsTaskCompleted(int, Set)} to keep
     * track of which Stats are missing or have irregular values.
     */
    enum Stat {
        PREV_CLOSE, OPEN, VOLUME, AVG_VOLUME,
        TODAYS_LOW, TODAYS_HIGH, FIFTY_TWO_WEEK_LOW, FIFTY_TWO_WEEK_HIGH,
        MARKET_CAP, PE_RATIO, EPS, YIELD, DESCRIPTION
    }

    double getOpen();

    void setOpen(final double open);

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

    String getVolume();

    void setVolume(final String volume);

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
