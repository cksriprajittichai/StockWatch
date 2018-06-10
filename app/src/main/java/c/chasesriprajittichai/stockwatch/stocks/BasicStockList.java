package c.chasesriprajittichai.stockwatch.stocks;

import java.util.ArrayList;

public final class BasicStockList extends ArrayList<BasicStock> {

    /**
     * @return A CSV string of the tickers of the stocks in mstocks.
     */
    public String getStockTickersAsCSV() {
        return String.join(",", getStockTickers());
    }

    /**
     * Stock data includes the stock's state, price, change point, and change percent.
     *
     * @return A CSV string of the data of the stocks in mstocks.
     */
    public String getStockDataAsCSV() {
        final ArrayList<BasicStock.State> states = getStockStates();
        final ArrayList<Double> prices = getStockPrices();
        final ArrayList<Double> changePoints = getStockChangePoints();
        final ArrayList<Double> changePercents = getStockChangePercents();

        final ArrayList<String> data = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            data.add(states.get(i).toString() + ',' + prices.get(i) + ',' +
                    changePoints.get(i) + ',' + changePercents.get(i));
        }

        return String.join(",", data);
    }

    private ArrayList<BasicStock.State> getStockStates() {
        final ArrayList<BasicStock.State> states = new ArrayList<>(size());
        for (final BasicStock s : this) {
            states.add(s.getState());
        }
        return states;
    }

    public ArrayList<String> getStockTickers() {
        final ArrayList<String> tickers = new ArrayList<>(size());
        for (final BasicStock s : this) {
            tickers.add(s.getTicker());
        }
        return tickers;
    }

    private ArrayList<Double> getStockPrices() {
        final ArrayList<Double> prices = new ArrayList<>(size());
        for (final BasicStock s : this) {
            prices.add(s.getPrice());
        }
        return prices;
    }

    private ArrayList<Double> getStockChangePoints() {
        final ArrayList<Double> changePoints = new ArrayList<>(size());
        for (final BasicStock s : this) {
            changePoints.add(s.getChangePoint());
        }
        return changePoints;
    }

    private ArrayList<Double> getStockChangePercents() {
        final ArrayList<Double> changePercents = new ArrayList<>(size());
        for (final BasicStock s : this) {
            changePercents.add(s.getChangePercent());
        }
        return changePercents;
    }
}
