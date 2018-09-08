package c.chasesriprajittichai.stockwatch.stocks;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Information about why this class contains only ConcreteStockWithEhVals can
 * be found at {@link ConcreteStockWithEhVals}.
 */
public final class ConcreteStockWithEhValsList extends ArrayList<ConcreteStockWithEhVals> {

    public ConcreteStockWithEhValsList() {
        super();
    }

    public ConcreteStockWithEhValsList(final Collection<ConcreteStockWithEhVals> c) {
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
     * includes the Stock's state, price, change point, change percent, extra
     * hours price, extra hours change point, and extra hours change percent. If
     * a Stock should not have extra hours values, the extra hours values of
     * that Stock should be 0.
     *
     * @return A TSV String of the data of the stocks in mstocks
     * @see ConcreteStockWithEhVals#ConcreteStockWithEhVals(Stock)
     */
    public String getStockDataAsTSV() {
        final List<Stock.State> states = getStockStates();
        final List<Double> prices = getStockPrices();
        final List<Double> changePoints = getStockChangePoints();
        final List<Double> changePercents = getStockChangePercents();
        final List<Double> ehPrices = getStockExtraHoursPrices();
        final List<Double> ehChangePoints = getStockExtraHoursChangePoints();
        final List<Double> ehChangePercents = getStockExtraHoursChangePercents();

        final List<String> data = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            data.add(states.get(i).toString() + '\t' +
                    prices.get(i) + '\t' +
                    changePoints.get(i) + '\t' +
                    changePercents.get(i) + '\t' +
                    ehPrices.get(i) + '\t' +
                    ehChangePoints.get(i) + '\t' +
                    ehChangePercents.get(i)
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

    private List<Double> getStockExtraHoursPrices() {
        final List<Double> prices = new ArrayList<>(size());
        for (final ConcreteStockWithEhVals s : this) {
            prices.add(s.getExtraHoursPrice());
        }
        return prices;
    }

    private List<Double> getStockExtraHoursChangePoints() {
        final List<Double> changePoints = new ArrayList<>(size());
        for (final ConcreteStockWithEhVals s : this) {
            changePoints.add(s.getExtraHoursChangePoint());
        }
        return changePoints;
    }

    private List<Double> getStockExtraHoursChangePercents() {
        final List<Double> changePercents = new ArrayList<>(size());
        for (final ConcreteStockWithEhVals s : this) {
            changePercents.add(s.getExtraHoursChangePercent());
        }
        return changePercents;
    }

}
