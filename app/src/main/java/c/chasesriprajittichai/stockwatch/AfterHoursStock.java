package c.chasesriprajittichai.stockwatch;

import java.util.ArrayList;

public class AfterHoursStock extends AdvancedStock {

    protected double close_price;
    protected double close_changePoint;
    protected double close_changePercent;

    public AfterHoursStock(State state, String ticker, String name, double price, double changePoint,
                           double changePercent, double close_price, double close_changePoint,
                           double close_changePercent, ArrayList<Double> yData) {
        super(state, ticker, name, price, changePoint, changePercent, yData);
        this.close_price = close_price;
        this.close_changePoint = close_changePoint;
        this.close_changePercent = close_changePercent;
    }

    public double getClose_price() {
        return close_price;
    }

    public void setClose_price(double close_price) {
        this.close_price = close_price;
    }

    public double getClose_changePoint() {
        return close_changePoint;
    }

    public void setClose_changePoint(double close_changePoint) {
        this.close_changePoint = close_changePoint;
    }

    public double getClose_changePercent() {
        return close_changePercent;
    }

    public void setClose_changePercent(double close_changePercent) {
        this.close_changePercent = close_changePercent;
    }
}
