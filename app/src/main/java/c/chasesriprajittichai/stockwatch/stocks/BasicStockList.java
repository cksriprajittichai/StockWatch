package c.chasesriprajittichai.stockwatch.stocks;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public final class BasicStockList extends ArrayList<BasicStock> {

    public BasicStockList() {
        super();
    }

    public BasicStockList(final Collection<BasicStock> c) {
        super(c);
    }

    /**
     * @return A TSV string of the tickers of the stocks in mstocks
     */
    public String getStockTickersAsTSV() {
        return TextUtils.join("\t", getStockTickers());
    }

    /**
     * @return A TSV string of the tickers of the stocks in mstocks
     */
    public String getStockNamesAsTSV() {
        return TextUtils.join("\t", getStockNames());
    }

    /**
     * Stock data includes the stock's state, price, change point, and change
     * percent.
     *
     * @return A TSV String of the data of the stocks in mstocks
     */
    public String getStockDataAsTSV() {
        final List<BasicStock.State> states = getStockStates();
        final List<Double> prices = getStockPrices();
        final List<Double> changePoints = getStockChangePoints();
        final List<Double> changePercents = getStockChangePercents();

        final List<String> data = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            data.add(states.get(i).toString() + '\t' +
                    prices.get(i) + '\t' +
                    changePoints.get(i) + '\t' +
                    changePercents.get(i));
        }

        return TextUtils.join("\t", data);
    }

    private List<BasicStock.State> getStockStates() {
        final List<BasicStock.State> states = new ArrayList<>(size());
        for (final BasicStock s : this) {
            states.add(s.getState());
        }
        return states;
    }

    private List<String> getStockTickers() {
        final List<String> tickers = new ArrayList<>(size());
        for (final BasicStock s : this) {
            tickers.add(s.getTicker());
        }
        return tickers;
    }

    private List<String> getStockNames() {
        final List<String> names = new ArrayList<>(size());
        for (final BasicStock s : this) {
            names.add(s.getName());
        }
        return names;
    }

    private List<Double> getStockPrices() {
        final List<Double> prices = new ArrayList<>(size());
        for (final BasicStock s : this) {
            prices.add(s.getPrice());
        }
        return prices;
    }

    private List<Double> getStockChangePoints() {
        final List<Double> changePoints = new ArrayList<>(size());
        for (final BasicStock s : this) {
            changePoints.add(s.getChangePoint());
        }
        return changePoints;
    }

    private List<Double> getStockChangePercents() {
        final List<Double> changePercents = new ArrayList<>(size());
        for (final BasicStock s : this) {
            changePercents.add(s.getChangePercent());
        }
        return changePercents;
    }
}
