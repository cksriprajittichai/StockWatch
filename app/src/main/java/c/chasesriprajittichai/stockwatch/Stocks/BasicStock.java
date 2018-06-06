package c.chasesriprajittichai.stockwatch.Stocks;

public class BasicStock {

    public enum State {PREMARKET, OPEN, AFTER_HOURS, CLOSED}

    protected State mstate;
    protected String mticker;
    protected double mprice;
    protected double mchangePoint;
    protected double mchangePercent;

    public BasicStock(State state, String ticker, double price, double changePoint, double changePercent) {
        this.mstate = state;
        this.mticker = ticker;
        this.mprice = price;
        this.mchangePoint = changePoint;
        this.mchangePercent = changePercent;
    }

    public State getState() {
        return mstate;
    }

    public void setState(State state) {
        this.mstate = state;
    }

    public String getTicker() {
        return mticker;
    }

    public void setTicker(String ticker) {
        this.mticker = ticker;
    }

    public double getPrice() {
        return mprice;
    }

    public void setPrice(double price) {
        this.mprice = price;
    }

    public double getChangePoint() {
        return mchangePoint;
    }

    public void setChangePoint(double changePoint) {
        this.mchangePoint = changePoint;
    }

    public double getChangePercent() {
        return mchangePercent;
    }

    public void setChangePercent(double changePercent) {
        this.mchangePercent = changePercent;
    }

}
