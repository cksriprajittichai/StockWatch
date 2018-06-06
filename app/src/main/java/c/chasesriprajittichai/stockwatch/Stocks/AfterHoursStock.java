package c.chasesriprajittichai.stockwatch.Stocks;

import java.util.ArrayList;

public class AfterHoursStock extends AdvancedStock {

    protected double mclose_price;
    protected double mclose_changePoint;
    protected double mclose_changePercent;

    public AfterHoursStock(State state, String ticker, String name, double price, double changePoint,
                           double changePercent, double close_price, double close_changePoint,
                           double close_changePercent, ArrayList<Double> yData) {
        super(state, ticker, name, price, changePoint, changePercent, yData);
        mclose_price = close_price;
        mclose_changePoint = close_changePoint;
        mclose_changePercent = close_changePercent;
    }

    public double getClose_price() {
        return mclose_price;
    }

    public void setClose_price(double close_price) {
        mclose_price = close_price;
    }

    public double getClose_changePoint() {
        return mclose_changePoint;
    }

    public void setClose_changePoint(double close_changePoint) {
        mclose_changePoint = close_changePoint;
    }

    public double getClose_changePercent() {
        return mclose_changePercent;
    }

    public void setClose_changePercent(double close_changePercent) {
        mclose_changePercent = close_changePercent;
    }
}
