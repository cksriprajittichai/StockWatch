package c.chasesriprajittichai.stockwatch;

public class BasicStock {

    public enum State {OPEN, AFTER_HOURS, CLOSED}

    protected State state;
    protected String ticker;
    protected double price;
    protected double changePoint;
    protected double changePercent;

    public BasicStock(State state, String ticker, double price, double changePoint, double changePercent) {
        this.state = state;
        this.ticker = ticker;
        this.price = price;
        this.changePoint = changePoint;
        this.changePercent = changePercent;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getChangePoint() {
        return changePoint;
    }

    public void setChangePoint(double changePoint) {
        this.changePoint = changePoint;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

}
