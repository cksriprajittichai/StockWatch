package c.chasesriprajittichai.stockwatch.stocks;

import java.util.ArrayList;
import java.util.List;


public final class ConcreteAdvancedStock extends ConcreteBasicStock implements AdvancedStock {

    public static final ConcreteAdvancedStock ERROR = new ConcreteAdvancedStock(BasicStock.State.ERROR, "",
            "", 0, 0, 0, 0, 0,
            0, 0, "", 0, 0, 0,
            0, "", "", new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>());

    private final double priceAtOpen;
    private final double todaysLow;
    private final double todaysHigh;
    private final double fiftyTwoWeekLow;
    private final double fiftyTwoWeekHigh;
    private final String marketCap;
    private final double prevClose;
    private final double peRatio;
    private final double eps;
    private final double yield;
    private final String avgVolume;
    private final String description;
    private final List<Double> prices_1day;
    private final List<Double> prices_2weeks;
    private final List<Double> prices_1month;
    private final List<Double> prices_3months;
    private final List<Double> prices_1year;
    private final List<Double> prices_5years;
    private final List<String> dates_2weeks;
    private final List<String> dates_1month;
    private final List<String> dates_3months;
    private final List<String> dates_1year;
    private final List<String> dates_5years;

    public ConcreteAdvancedStock(final State state, final String ticker, final String name,
                                 final double price, final double changePoint,
                                 final double changePercent, final double todaysLow,
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
        super(state, ticker, name, price, changePoint, changePercent);
        this.todaysLow = todaysLow;
        this.todaysHigh = todaysHigh;
        this.fiftyTwoWeekLow = fiftyTwoWeekLow;
        this.fiftyTwoWeekHigh = fiftyTwoWeekHigh;
        this.marketCap = marketCap;
        this.prevClose = prevClose;
        this.peRatio = peRatio;
        this.eps = eps;
        this.yield = yield;
        avgVolume = averageVolume;
        this.description = description;
        this.prices_1day = prices_1day;
        priceAtOpen = !this.prices_1day.isEmpty() ? this.prices_1day.get(0) : -1;
        this.prices_2weeks = prices_2weeks;
        this.prices_1month = prices_1month;
        this.prices_3months = prices_3months;
        this.prices_1year = prices_1year;
        this.prices_5years = prices_5years;
        this.dates_2weeks = dates_2weeks;
        this.dates_1month = dates_1month;
        this.dates_3months = dates_3months;
        this.dates_1year = dates_1year;
        this.dates_5years = dates_5years;
    }

    @Override
    public double getPriceAtOpen() {
        return priceAtOpen;
    }

    @Override
    public double getTodaysLow() {
        return todaysLow;
    }

    @Override
    public double getTodaysHigh() {
        return todaysHigh;
    }

    @Override
    public double getFiftyTwoWeekLow() {
        return fiftyTwoWeekLow;
    }

    @Override
    public double getFiftyTwoWeekHigh() {
        return fiftyTwoWeekHigh;
    }

    @Override
    public String getMarketCap() {
        return marketCap;
    }

    @Override
    public double getPrevClose() {
        return prevClose;
    }

    @Override
    public double getPeRatio() {
        return peRatio;
    }

    @Override
    public double getEps() {
        return eps;
    }

    @Override
    public double getYield() {
        return yield;
    }

    @Override
    public String getAverageVolume() {
        return avgVolume;
    }

    @Override
    public String getDescription() {
        return description;
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
    public List<Double> getPrices_2weeks() {
        return prices_2weeks;
    }

    @Override
    public List<Double> getPrices_1month() {
        return prices_1month;
    }

    @Override
    public List<Double> getPrices_3months() {
        return prices_3months;
    }

    @Override
    public List<Double> getPrices_1year() {
        return prices_1year;
    }

    @Override
    public List<Double> getPrices_5years() {
        return prices_5years;
    }

    @Override
    public List<String> getDates_2weeks() {
        return dates_2weeks;
    }

    @Override
    public List<String> getDates_1month() {
        return dates_1month;
    }

    @Override
    public List<String> getDates_3months() {
        return dates_3months;
    }

    @Override
    public List<String> getDates_1year() {
        return dates_1year;
    }

    @Override
    public List<String> getDates_5years() {
        return dates_5years;
    }

}
