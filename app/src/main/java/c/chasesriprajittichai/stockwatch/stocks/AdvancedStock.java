package c.chasesriprajittichai.stockwatch.stocks;

import java.util.ArrayList;
import java.util.Collections;

public class AdvancedStock extends BasicStock {

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
    private final ArrayList<Double> myData;

    public AdvancedStock(final State state, final String ticker, final String name,
                         final double price, final double changePoint, final double changePercent,
                         final double openPrice, final double dayRangeLow, final double dayRangeHigh,
                         final double fiftyTwoWeekRangeLow, final double fiftyTwoWeekRangeHigh,
                         final String marketCap, final double beta, final double peRatio,
                         final double eps, final double yield, final String averageVolume,
                         final String description, final ArrayList<Double> yData) {
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
        myData = yData;
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

    public ArrayList<Double> getYData() {
        return myData;
    }

    public void setYData(final ArrayList<Double> yData) {
        myData.clear();
        Collections.copy(myData, yData);
    }

}
