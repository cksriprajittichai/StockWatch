package c.chasesriprajittichai.stockwatch.stocks;

public class BasicStock {

    public enum State {PREMARKET, OPEN, AFTER_HOURS, CLOSED}

    private State mstate;
    private final String mticker;
    private double mprice;
    private double mchangePoint;
    private double mchangePercent;

    public BasicStock(final State state, final String ticker, final double price,
                      final double changePoint, final double changePercent) {
        mstate = state;
        mticker = ticker;
        mprice = price;
        mchangePoint = changePoint;
        mchangePercent = changePercent;
    }

    public State getState() {
        return mstate;
    }

    public void setState(final State state) {
        mstate = state;
    }

    public String getTicker() {
        return mticker;
    }

    public double getPrice() {
        return mprice;
    }

    public void setPrice(final double price) {
        mprice = price;
    }

    public double getChangePoint() {
        return mchangePoint;
    }

    public void setChangePoint(final double changePoint) {
        mchangePoint = changePoint;
    }

    public double getChangePercent() {
        return mchangePercent;
    }

    public void setChangePercent(final double changePercent) {
        mchangePercent = changePercent;
    }

}
