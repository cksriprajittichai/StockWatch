package c.chasesriprajittichai.stockwatch.stocks;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Information about why this class contains only ConcreteStockWithAhVals can
 * be found at {@link ConcreteStockWithAhVals}.
 */
public final class ConcreteStockWithAhValsList extends ArrayList<ConcreteStockWithAhVals> {

    public ConcreteStockWithAhValsList() {
        super();
    }

    public ConcreteStockWithAhValsList(final Collection<ConcreteStockWithAhVals> c) {
        super(c);
    }

    /**
     * @return A TSV string of the tickers of the Stocks in this list
     */
    public String getStockTickersAsTSV() {
        return TextUtils.join("\t", getStockTickers());
    }

    /**
     * @return A TSV string of the tickers of the Stocks in this list
     */
    public String getStockNamesAsTSV() {
        return TextUtils.join("\t", getStockNames());
    }

    /**
     * Returns a list containing the data of the Stocks in this list. Stock data
     * includes the Stock's state, price, change point, change percent, after
     * hours price, after hours change point, and after hours change percent. If
     * a Stock should not have after hours values, the after hours values of
     * that Stock should be 0.
     *
     * @return A TSV String of the data of the stocks in mstocks
     * @see ConcreteStockWithAhVals#ConcreteStockWithAhVals(Stock)
     */
    public String getStockDataAsTSV() {
        final List<Stock.State> states = getStockStates();
        final List<Double> prices = getStockPrices();
        final List<Double> changePoints = getStockChangePoints();
        final List<Double> changePercents = getStockChangePercents();
        final List<Double> ahPrices = getStockAfterHoursPrices();
        final List<Double> ahChangePoints = getStockAfterHoursChangePoints();
        final List<Double> ahChangePercents = getStockAfterHoursChangePercents();

        final List<String> data = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            data.add(states.get(i).toString() + '\t' +
                    prices.get(i) + '\t' +
                    changePoints.get(i) + '\t' +
                    changePercents.get(i) + '\t' +
                    ahPrices.get(i) + '\t' +
                    ahChangePoints.get(i) + '\t' +
                    ahChangePercents.get(i)
            );
        }

        return TextUtils.join("\t", data);
    }

    private List<Stock.State> getStockStates() {
        final List<Stock.State> states = new ArrayList<>(size());
        for (final Stock s : this) {
            states.add(s.getState());
        }
        return states;
    }

    private List<String> getStockTickers() {
        final List<String> tickers = new ArrayList<>(size());
        for (final Stock s : this) {
            tickers.add(s.getTicker());
        }
        return tickers;
    }

    private List<String> getStockNames() {
        final List<String> names = new ArrayList<>(size());
        for (final Stock s : this) {
            names.add(s.getName());
        }
        return names;
    }

    private List<Double> getStockPrices() {
        final List<Double> prices = new ArrayList<>(size());
        for (final Stock s : this) {
            prices.add(s.getPrice());
        }
        return prices;
    }

    private List<Double> getStockChangePoints() {
        final List<Double> changePoints = new ArrayList<>(size());
        for (final Stock s : this) {
            changePoints.add(s.getChangePoint());
        }
        return changePoints;
    }

    private List<Double> getStockChangePercents() {
        final List<Double> changePercents = new ArrayList<>(size());
        for (final Stock s : this) {
            changePercents.add(s.getChangePercent());
        }
        return changePercents;
    }

    private List<Double> getStockAfterHoursPrices() {
        final List<Double> prices = new ArrayList<>(size());
        for (final ConcreteStockWithAhVals s : this) {
            prices.add(s.getAfterHoursPrice());
        }
        return prices;
    }

    private List<Double> getStockAfterHoursChangePoints() {
        final List<Double> changePoints = new ArrayList<>(size());
        for (final ConcreteStockWithAhVals s : this) {
            changePoints.add(s.getAfterHoursChangePoint());
        }
        return changePoints;
    }

    private List<Double> getStockAfterHoursChangePercents() {
        final List<Double> changePercents = new ArrayList<>(size());
        for (final ConcreteStockWithAhVals s : this) {
            changePercents.add(s.getAfterHoursChangePercent());
        }
        return changePercents;
    }

}
