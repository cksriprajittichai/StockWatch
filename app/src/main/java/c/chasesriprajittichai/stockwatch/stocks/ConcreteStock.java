package c.chasesriprajittichai.stockwatch.stocks;

import c.chasesriprajittichai.stockwatch.HomeActivity;


/**
 * Information about why this class is not used in {@link HomeActivity#rv} can
 * be found at {@link ConcreteStockWithAhVals}.
 */
public class ConcreteStock implements Stock, StockInHomeActivity {

    private State state;
    private final String ticker;
    private final String name;
    private double price;
    private double changePoint;
    private double changePercent;

    public ConcreteStock(final State state, final String ticker,
                         final String name, final double price,
                         final double changePoint, final double changePercent) {
        this.state = state;
        this.ticker = ticker;
        this.name = name;
        this.price = price;
        this.changePoint = changePoint;
        this.changePercent = changePercent;
    }

    /**
     * Copy constructor.
     *
     * @param stock The Stock to copy
     */
    public ConcreteStock(final Stock stock) {
        state = stock.getState();
        ticker = stock.getTicker();
        name = stock.getName();
        price = stock.getPrice();
        changePoint = stock.getChangePoint();
        changePercent = stock.getChangePercent();
    }

    /**
     * @return The price of the Stock
     */
    @Override
    public double getLivePrice() {
        return price;
    }

    /**
     * @return The change point from the open trading hours
     */
    @Override
    public double getLiveChangePoint() {
        return changePoint;
    }

    /**
     * @return The change percent from the open trading hours
     */
    @Override
    public double getLiveChangePercent() {
        return changePercent;
    }

    /**
     * @return The change percent during the open trading hours
     */
    @Override
    public double getNetChangePercent() {
        return changePercent;
    }

    /**
     * @return A four element string array containing the {@link
     * StockInHomeActivity}'s {@link Stock.State}, price, change point, and
     * change percent.
     */
    @Override
    public String[] getDataAsArray() {
        final String[] data = new String[4];
        data[0] = state.toString();
        data[1] = String.valueOf(price);
        data[2] = String.valueOf(changePoint);
        data[3] = String.valueOf(changePercent);
        return data;
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

}
