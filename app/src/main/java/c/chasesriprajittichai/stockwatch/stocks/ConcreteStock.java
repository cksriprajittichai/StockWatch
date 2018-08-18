package c.chasesriprajittichai.stockwatch.stocks;

import android.text.NoCopySpan;


/**
 * Some useful information about this class can be found at {@link
 * ConcreteStockWithAhVals}.
 */
public class ConcreteStock implements Stock, StockInHomeActivity {

    private State state;
    private final String ticker;
    private final String name;
    private double price;
    private double changePoint;
    private double changePercent;

    public ConcreteStock(final State state, final String ticker,
                         final String name, final double price,
                         final double changePoint, final double changePercent) {
        this.state = state;
        this.ticker = ticker;
        this.name = name;
        this.price = price;
        this.changePoint = changePoint;
        this.changePercent = changePercent;
    }

    public ConcreteStock(final Stock stock) {
        state = stock.getState();
        ticker = stock.getTicker();
        name = stock.getName();
        price = stock.getPrice();
        changePoint = stock.getChangePoint();
        changePercent = stock.getChangePercent();
    }

    @Override
    public double getLivePrice() {
        return price;
    }

    @Override
    public double getLiveChangePoint() {
        return changePoint;
    }

    @Override
    public double getLiveChangePercent() {
        return changePercent;
    }

    @Override
    public final State getState() {
        return state;
    }

    @Override
    public final void setState(final State state) {
        this.state = state;
    }

    @Override
    public final String getTicker() {
        return ticker;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final double getPrice() {
        return price;
    }

    @Override
    public final void setPrice(final double price) {
        this.price = price;
    }

    @Override
    public final double getChangePoint() {
        return changePoint;
    }

    @Override
    public final void setChangePoint(final double changePoint) {
        this.changePoint = changePoint;
    }

    @Override
    public final double getChangePercent() {
        return changePercent;
    }

    @Override
    public final void setChangePercent(final double changePercent) {
        this.changePercent = changePercent;
    }

    @Override
    public double getNetChangePoint() {
        return changePoint;
    }

    @Override
    public double getNetChangePercent() {
        return changePercent;
    }

    @Override
    public String[] getDataAsArray() {
        String[] data = new String[4];
        data[0] = state.toString();
        data[1] = String.valueOf(price);
        data[2] = String.valueOf(changePoint);
        data[3] = String.valueOf(changePercent);
        return data;
    }
}
