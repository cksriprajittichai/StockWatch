package c.chasesriprajittichai.stockwatch.stocks;

public class BasicStock {

    public enum State {PREMARKET, OPEN, AFTER_HOURS, CLOSED}

    private State mstate;
    private String mticker;
    private double mprice;
    private double mchangePoint;
    private double mchangePercent;

    public BasicStock(State state, String ticker, double price, double changePoint, double changePercent) {
        mstate = state;
        mticker = ticker;
        mprice = price;
        mchangePoint = changePoint;
        mchangePercent = changePercent;
    }

    public State getState() {
        return mstate;
    }

    public void setState(State state) {
        mstate = state;
    }

    public String getTicker() {
        return mticker;
    }

    public void setTicker(String ticker) {
        mticker = ticker;
    }

    public double getPrice() {
        return mprice;
    }

    public void setPrice(double price) {
        mprice = price;
    }

    public double getChangePoint() {
        return mchangePoint;
    }

    public void setChangePoint(double changePoint) {
        mchangePoint = changePoint;
    }

    public double getChangePercent() {
        return mchangePercent;
    }

    public void setChangePercent(double changePercent) {
        mchangePercent = changePercent;
    }

}
