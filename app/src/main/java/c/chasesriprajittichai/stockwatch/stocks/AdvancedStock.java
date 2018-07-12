package c.chasesriprajittichai.stockwatch.stocks;

import java.util.ArrayList;
import java.util.List;

public class AdvancedStock extends BasicStock {

    public enum ChartPeriod {ONE_DAY, TWO_WEEKS, ONE_MONTH, THREE_MONTHS, ONE_YEAR, FIVE_YEARS}

    public static AdvancedStock ERROR_AdvancedStock = new AdvancedStock(State.ERROR, "",
            "", 0, 0, 0, 0, 0, 0,
            0, 0, "", 0, 0, 0,
            0, "", "", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

    private final String mname;
    private final double mopenPrice;
    private double mdayRangeLow;
    private double mdayRangeHigh;
    private final double mfiftyTwoWeekRangeLow;
    private final double mfiftyTwoWeekRangeHigh;
    private final String mmarketCap;
    private final double mbeta;
    private final double mpeRatio;
    private final double meps;
    private final double myield;
    private final String mavgVolume;
    private final String mdescription;
    private final List<Double> myData_1day;
    private final List<Double> myData_2weeks;
    private final List<Double> myData_1month;
    private final List<Double> myData_3months;
    private final List<Double> myData_1year;
    private final List<Double> myData_5years;
    private final List<String> mdates_2weeks;
    private final List<String> mdates_1month;
    private final List<String> mdates_3months;
    private final List<String> mdates_1year;
    private final List<String> mdates_5years;

    public AdvancedStock(final State state, final String ticker, final String name,
                         final double price, final double changePoint, final double changePercent,
                         final double openPrice, final double dayRangeLow, final double dayRangeHigh,
                         final double fiftyTwoWeekRangeLow, final double fiftyTwoWeekRangeHigh,
                         final String marketCap, final double beta, final double peRatio,
                         final double eps, final double yield, final String averageVolume,
                         final String description, final List<Double> yData_1day,
                         final List<Double> yData_2weeks, final List<Double> yData_1month,
                         final List<Double> yData_3months, final List<Double> yData_1year,
                         final List<Double> yData_5years, final List<String> dates_2weeks,
                         final List<String> dates_1month, final List<String> dates_3months,
                         final List<String> dates_1year, final List<String> dates_5years) {
        super(state, ticker, price, changePoint, changePercent);
        mname = name;
        mopenPrice = openPrice;
        mdayRangeLow = dayRangeLow;
        mdayRangeHigh = dayRangeHigh;
        mfiftyTwoWeekRangeLow = fiftyTwoWeekRangeLow;
        mfiftyTwoWeekRangeHigh = fiftyTwoWeekRangeHigh;
        mmarketCap = marketCap;
        mbeta = beta;
        mpeRatio = peRatio;
        meps = eps;
        myield = yield;
        mavgVolume = averageVolume;
        mdescription = description;
        myData_1day = yData_1day;
        myData_2weeks = yData_2weeks;
        myData_1month = yData_1month;
        myData_3months = yData_3months;
        myData_1year = yData_1year;
        myData_5years = yData_5years;
        mdates_2weeks = dates_2weeks;
        mdates_1month = dates_1month;
        mdates_3months = dates_3months;
        mdates_1year = dates_1year;
        mdates_5years = dates_5years;
    }

    public String getName() {
        return mname;
    }

    public double getOpenPrice() {
        return mopenPrice;
    }

    public double getDayRangeLow() {
        return mdayRangeLow;
    }

    public double getDayRangeHigh() {
        return mdayRangeHigh;
    }

    public double getFiftyTwoWeekRangeLow() {
        return mfiftyTwoWeekRangeLow;
    }

    public double getFiftyTwoWeekRangeHigh() {
        return mfiftyTwoWeekRangeHigh;
    }

    public String getMarketCap() {
        return mmarketCap;
    }

    public double getBeta() {
        return mbeta;
    }

    public double getPeRatio() {
        return mpeRatio;
    }

    public double getEps() {
        return meps;
    }

    public double getYield() {
        return myield;
    }

    public String getAverageVolume() {
        return mavgVolume;
    }

    public String getDescription() {
        return mdescription;
    }

    public List<Double> getYData_1day() {
        return myData_1day;
    }

    public List<Double> getYData_2weeks() {
        return myData_2weeks;
    }

    public List<Double> getYData_1month() {
        return myData_1month;
    }

    public List<Double> getYData_3months() {
        return myData_3months;
    }

    public List<Double> getYData_1year() {
        return myData_1year;
    }

    public List<Double> getYData_5years() {
        return myData_5years;
    }

    public List<String> getDates_2weeks() {
        return mdates_2weeks;
    }

    public List<String> getDates_1month() {
        return mdates_1month;
    }

    public List<String> getDates_3months() {
        return mdates_3months;
    }

    public List<String> getDates_1year() {
        return mdates_1year;
    }

    public List<String> getDates_5years() {
        return mdates_5years;
    }

}
