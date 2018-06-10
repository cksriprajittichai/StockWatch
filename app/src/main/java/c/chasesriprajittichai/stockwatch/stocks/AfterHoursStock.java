package c.chasesriprajittichai.stockwatch.stocks;

import java.util.ArrayList;

public final class AfterHoursStock extends AdvancedStock {

    private double mclose_price;
    private double mclose_changePoint;
    private double mclose_changePercent;

    public AfterHoursStock(final State state, final String ticker, final String name,
                           final double price, final double changePoint, final double changePercent,
                           final double close_price, final double close_changePoint,
                           final double close_changePercent, final String description,
                           final ArrayList<Double> yData) {
        super(state, ticker, name, price, changePoint, changePercent, description, yData);
        mclose_price = close_price;
        mclose_changePoint = close_changePoint;
        mclose_changePercent = close_changePercent;
    }

    public double getClose_price() {
        return mclose_price;
    }

    public void setClose_price(final double close_price) {
        mclose_price = close_price;
    }

    public double getClose_changePoint() {
        return mclose_changePoint;
    }

    public void setClose_changePoint(final double close_changePoint) {
        mclose_changePoint = close_changePoint;
    }

    public double getClose_changePercent() {
        return mclose_changePercent;
    }

    public void setClose_changePercent(final double close_changePercent) {
        mclose_changePercent = close_changePercent;
    }
}
