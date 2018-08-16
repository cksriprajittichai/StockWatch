package c.chasesriprajittichai.stockwatch.stocks;

public class ConcreteBasicStockWithAhVals
        extends ConcreteBasicStock
        implements BasicStock, StockWithAhVals {

    private double ahPrice;

    private double ahChangePoint;

    private double ahChangePercent;

    public ConcreteBasicStockWithAhVals(final State state, final String ticker,
                                        final String name, final double price,
                                        final double changePoint,
                                        final double changePercent,
                                        final double afterHoursPrice,
                                        final double afterHoursChangePoint,
                                        final double afterHoursChangePercent) {
        super(state, ticker, name, price, changePoint, changePercent);
        ahPrice = afterHoursPrice;
        ahChangePoint = afterHoursChangePoint;
        ahChangePercent = afterHoursChangePercent;
    }

    @Override
    public final double getLivePrice() {
        return ahPrice;
    }

    @Override
    public final double getLiveChangePoint() {
        return ahChangePoint;
    }

    @Override
    public final double getLiveChangePercent() {
        return ahChangePercent;
    }

    @Override
    public final double getNetChangePoint() {
        return getChangePoint() + ahChangePoint;
    }

    @Override
    public final double getNetChangePercent() {
        return getChangePercent() + ahChangePercent;
    }

    @Override
    public final double getAfterHoursPrice() {
        return ahPrice;
    }

    @Override
    public final void setAfterHoursPrice(final double afterHoursPrice) {
        ahPrice = afterHoursPrice;
    }

    @Override
    public final double getAfterHoursChangePoint() {
        return ahChangePoint;
    }

    @Override
    public final void setAfterHoursChangePoint(final double afterHoursChangePoint) {
        ahChangePoint = afterHoursChangePoint;
    }

    @Override
    public final double getAfterHoursChangePercent() {
        return ahChangePercent;
    }

    @Override
    public final void setAfterHoursChangePercent(final double afterHoursChangePercent) {
        ahChangePercent = afterHoursChangePercent;
    }

}
