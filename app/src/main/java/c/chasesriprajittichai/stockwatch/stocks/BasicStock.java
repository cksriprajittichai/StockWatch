package c.chasesriprajittichai.stockwatch.stocks;


public class BasicStock {

    public enum State {ERROR, PREMARKET, OPEN, AFTER_HOURS, CLOSED}

    private State state;
    private final String ticker;
    private final String name;
    private double price;
    private double changePoint;
    private double changePercent;

    public BasicStock(final State state, final String ticker, final String name,
                      final double price, final double changePoint,
                      final double changePercent) {
        this.state = state;
        this.ticker = ticker;
        this.name = name;
        this.price = price;
        this.changePoint = changePoint;
        this.changePercent = changePercent;
    }

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(final double price) {
        this.price = price;
    }

    public double getChangePoint() {
        return changePoint;
    }

    public void setChangePoint(final double changePoint) {
        this.changePoint = changePoint;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(final double changePercent) {
        this.changePercent = changePercent;
    }

}
