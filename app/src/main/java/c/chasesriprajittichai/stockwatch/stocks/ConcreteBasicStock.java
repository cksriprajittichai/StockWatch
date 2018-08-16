package c.chasesriprajittichai.stockwatch.stocks;


public class ConcreteBasicStock implements BasicStock {

    private State state;
    private final String ticker;
    private final String name;
    private double price;
    private double changePoint;
    private double changePercent;

    public ConcreteBasicStock(final State state, final String ticker,
                              final String name, final double price,
                              final double changePoint, final double changePercent) {
        this.state = state;
        this.ticker = ticker;
        this.name = name;
        this.price = price;
        this.changePoint = changePoint;
        this.changePercent = changePercent;
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
    public double getNetChangePoint() {
        return changePoint;
    }

    @Override
    public double getNetChangePercent() {
        return changePercent;
    }

}
