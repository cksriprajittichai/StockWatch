package c.chasesriprajittichai.stockwatch.stocks;

import java.util.ArrayList;
import java.util.List;

import c.chasesriprajittichai.stockwatch.IndividualStockActivity;


public final class ConcreteAdvancedStock extends ConcreteStock implements AdvancedStock {

    private double prevClose;
    private double open;
    private String volume;
    private String avgVolume;
    private double todaysLow;
    private double todaysHigh;
    private double fiftyTwoWeekLow;
    private double fiftyTwoWeekHigh;
    private String marketCap;
    private double peRatio;
    private double eps;
    private double yield;
    private String description;
    private List<Double> prices_1day;
    private List<Double> prices_2weeks;
    private List<Double> prices_1month;
    private List<Double> prices_3months;
    private List<Double> prices_1year;
    private List<Double> prices_5years;
    private List<String> dates_2weeks;
    private List<String> dates_1month;
    private List<String> dates_3months;
    private List<String> dates_1year;
    private List<String> dates_5years;

    public ConcreteAdvancedStock(final State state, final String ticker,
                                 final String name, final double price,
                                 final double changePoint, final double changePercent) {
        super(state, ticker, name, price, changePoint, changePercent);
    }

    /**
     * Copy constructor. Used in {@link IndividualStockActivity} to convert
     * {@link IndividualStockActivity#stock} - which was a {@link
     * ConcreteAdvancedStockWithEhVals} - into a ConcreteAdvancedStock.
     *
     * @param stock The AdvancedStock to copy
     */
    public ConcreteAdvancedStock(final AdvancedStock stock) {
        super(stock.getState(), stock.getTicker(), stock.getName(),
                stock.getPrice(), stock.getChangePoint(), stock.getChangePercent());
        prevClose = stock.getPrevClose();
        open = stock.getOpen();
        volume = stock.getVolume();
        avgVolume = stock.getAverageVolume();
        todaysLow = stock.getTodaysLow();
        todaysHigh = stock.getTodaysHigh();
        fiftyTwoWeekLow = stock.getFiftyTwoWeekLow();
        fiftyTwoWeekHigh = stock.getFiftyTwoWeekHigh();
        marketCap = stock.getMarketCap();
        peRatio = stock.getPeRatio();
        eps = stock.getEps();
        yield = stock.getYield();
        description = stock.getDescription();
        prices_1day = stock.getPrices_1day();
        prices_2weeks = stock.getPrices_2weeks();
        prices_1month = stock.getPrices_1month();
        prices_3months = stock.getPrices_3months();
        prices_1year = stock.getPrices_1year();
        prices_5years = stock.getPrices_5years();
        dates_2weeks = stock.getDates_2weeks();
        dates_1month = stock.getDates_1month();
        dates_3months = stock.getDates_3months();
        dates_1year = stock.getDates_1year();
        dates_5years = stock.getDates_5years();
    }

    @Override
    public double getPrevClose() {
        return prevClose;
    }

    @Override
    public void setPrevClose(final double prevClose) {
        this.prevClose = prevClose;
    }

    @Override
    public double getOpen() {
        return open;
    }

    @Override
    public void setOpen(final double open) {
        this.open = open;
    }

    @Override
    public String getVolume() {
        return volume;
    }

    @Override
    public void setVolume(final String volume) {
        this.volume = volume;
    }

    @Override
    public String getAverageVolume() {
        return avgVolume;
    }

    @Override
    public void setAverageVolume(final String avgVolume) {
        this.avgVolume = avgVolume;
    }

    @Override
    public double getTodaysLow() {
        return todaysLow;
    }

    @Override
    public void setTodaysLow(final double todaysLow) {
        this.todaysLow = todaysLow;
    }

    @Override
    public double getTodaysHigh() {
        return todaysHigh;
    }

    @Override
    public void setTodaysHigh(final double todaysHigh) {
        this.todaysHigh = todaysHigh;
    }

    @Override
    public double getFiftyTwoWeekLow() {
        return fiftyTwoWeekLow;
    }

    @Override
    public void setFiftyTwoWeekLow(final double fiftyTwoWeekLow) {
        this.fiftyTwoWeekLow = fiftyTwoWeekLow;
    }

    @Override
    public double getFiftyTwoWeekHigh() {
        return fiftyTwoWeekHigh;
    }

    @Override
    public void setFiftyTwoWeekHigh(final double fiftyTwoWeekHigh) {
        this.fiftyTwoWeekHigh = fiftyTwoWeekHigh;
    }

    @Override
    public String getMarketCap() {
        return marketCap;
    }

    @Override
    public void setMarketCap(final String marketCap) {
        this.marketCap = marketCap;
    }

    @Override
    public double getPeRatio() {
        return peRatio;
    }

    @Override
    public void setPeRatio(final double peRatio) {
        this.peRatio = peRatio;
    }

    @Override
    public double getEps() {
        return eps;
    }

    @Override
    public void setEps(final double eps) {
        this.eps = eps;
    }

    @Override
    public double getYield() {
        return yield;
    }

    @Override
    public void setYield(final double yield) {
        this.yield = yield;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public List<Double> getPrices(final ChartPeriod chartPeriod) {
        final List<Double> ret;

        switch (chartPeriod) {
            case ONE_DAY:
                ret = prices_1day;
                break;
            case TWO_WEEKS:
                ret = prices_2weeks;
                break;
            case ONE_MONTH:
                ret = prices_1month;
                break;
            case THREE_MONTHS:
                ret = prices_3months;
                break;
            case ONE_YEAR:
                ret = prices_1year;
                break;
            case FIVE_YEARS:
                ret = prices_5years;
                break;
            default:
                ret = new ArrayList<>();
                break;
        }

        return ret;
    }

    @Override
    public List<String> getDates(final ChartPeriod chartPeriod) {
        final List<String> ret;

        switch (chartPeriod) {
            case TWO_WEEKS:
                ret = dates_2weeks;
                break;
            case ONE_MONTH:
                ret = dates_1month;
                break;
            case THREE_MONTHS:
                ret = dates_3months;
                break;
            case ONE_YEAR:
                ret = dates_1year;
                break;
            case FIVE_YEARS:
                ret = dates_5years;
                break;
            default:
                ret = new ArrayList<>();
                break;
        }

        return ret;
    }

    @Override
    public List<Double> getPrices_1day() {
        return prices_1day;
    }

    @Override
    public void setPrices_1day(final List<Double> prices) {
        prices_1day = prices;
    }

    @Override
    public List<Double> getPrices_2weeks() {
        return prices_2weeks;
    }

    @Override
    public void setPrices_2weeks(final List<Double> prices) {
        prices_2weeks = prices;
    }

    @Override
    public List<Double> getPrices_1month() {
        return prices_1month;
    }

    @Override
    public void setPrices_1month(final List<Double> prices) {
        prices_1month = prices;
    }

    @Override
    public List<Double> getPrices_3months() {
        return prices_3months;
    }

    @Override
    public void setPrices_3months(final List<Double> prices) {
        prices_3months = prices;
    }

    @Override
    public List<Double> getPrices_1year() {
        return prices_1year;
    }

    @Override
    public void setPrices_1year(final List<Double> prices) {
        prices_1year = prices;
    }

    @Override
    public List<Double> getPrices_5years() {
        return prices_5years;
    }

    @Override
    public void setPrices_5years(final List<Double> prices) {
        prices_5years = prices;
    }

    @Override
    public List<String> getDates_2weeks() {
        return dates_2weeks;
    }

    @Override
    public void setDates_2weeks(final List<String> dates) {
        dates_2weeks = dates;
    }

    @Override
    public List<String> getDates_1month() {
        return dates_1month;
    }

    @Override
    public void setDates_1month(final List<String> dates) {
        dates_1month = dates;
    }

    @Override
    public List<String> getDates_3months() {
        return dates_3months;
    }

    @Override
    public void setDates_3months(final List<String> dates) {
        dates_3months = dates;
    }

    @Override
    public List<String> getDates_1year() {
        return dates_1year;
    }

    @Override
    public void setDates_1year(final List<String> dates) {
        dates_1year = dates;
    }

    @Override
    public List<String> getDates_5years() {
        return dates_5years;
    }

    @Override
    public void setDates_5years(final List<String> dates) {
        dates_5years = dates;
    }

}
