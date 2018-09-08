package c.chasesriprajittichai.stockwatch.stocks;

import c.chasesriprajittichai.stockwatch.HomeActivity;


/**
 * Regarding the {@link HomeActivity#rv}, operations on rv will be much faster
 * if rv only contains one type of object. If an element at an index in rv
 * changes type, expensive operations must be done to account for it. For this
 * reason, rv in HomeActivity only uses ConcreteStockWithEhVals, rather than
 * using both {@link ConcreteStock} and ConcreteStockWithEhVals. Additionally,
 * ConcreteStockList has been converted to {@link ConcreteStockWithEhValsList}
 * because of this.
 */
public class ConcreteStockWithEhVals
        extends ConcreteStock
        implements Stock, StockWithEhVals, StockInHomeActivity {

    /**
     * Price in extra hours; live price
     */
    private double ehPrice;

    /**
     * The change point that has occurred during extra hours trading
     */
    private double ehChangePoint;

    /**
     * The change percent that has occurred during extra hours trading
     */
    private double ehChangePercent;

    public ConcreteStockWithEhVals(final State state, final String ticker,
                                   final String name, final double price,
                                   final double changePoint,
                                   final double changePercent,
                                   final double extraHoursPrice,
                                   final double extraHoursChangePoint,
                                   final double extraHoursChangePercent) {
        super(state, ticker, name, price, changePoint, changePercent);
        ehPrice = extraHoursPrice;
        ehChangePoint = extraHoursChangePoint;
        ehChangePercent = extraHoursChangePercent;
    }

    /**
     * Copy constructor. If stock instanceof {@link StockWithEhVals}, this
     * ConcreteStockWithEhVals' extra hours values are set to the extra hours
     * values of stock. Otherwise, this ConcreteStockWithEhVals' extra hours
     * values are set to 0.
     *
     * @param stock The Stock to copy
     */
    public ConcreteStockWithEhVals(final Stock stock) {
        super(stock);

        if (stock instanceof StockWithEhVals) {
            final StockWithEhVals ehStock = (StockWithEhVals) stock;
            ehPrice = ehStock.getExtraHoursPrice();
            ehChangePoint = ehStock.getExtraHoursChangePoint();
            ehChangePercent = ehStock.getExtraHoursChangePercent();
        } else {
            ehPrice = 0;
            ehChangePoint = 0;
            ehChangePercent = 0;
        }
    }

    /**
     * Because this is the only non-AdvancedStock used in HomeActivity, this
     * class must be able to represent stocks that should have extra hours
     * values and stocks that shouldn't. In the code in HomeActivity and
     * MultiStockRequest, if a ConcreteStockWithEhVals shouldn't have extra
     * hours values, the extra hours values are set to 0. Checking if a
     * ConcreteStockWithEhVals should have extra hours values or not can
     * therefore be done by checking if the extra hours price is equal to 0.
     *
     * @return The live price
     */
    @Override
    public final double getLivePrice() {
        return ehPrice == 0 ? getPrice() : ehPrice;
    }

    /**
     * Because this is the only non-AdvancedStock used in HomeActivity, this
     * class must be able to represent stocks that should have extra hours
     * values and stocks that shouldn't. In the code in HomeActivity and
     * MultiStockRequest, if a ConcreteStockWithEhVals shouldn't have extra
     * hours values, the extra hours values are set to 0. Checking if a
     * ConcreteStockWithEhVals should have extra hours values or not can
     * therefore be done by checking if the extra hours price is equal to 0.
     *
     * @return The live change point
     */
    @Override
    public final double getLiveChangePoint() {
        return ehPrice == 0 ? getChangePoint() : ehChangePoint;
    }

    /**
     * Because this is the only non-AdvancedStock used in HomeActivity, this
     * class must be able to represent stocks that should have extra hours
     * values and stocks that shouldn't. In the code in HomeActivity and
     * MultiStockRequest, if a ConcreteStockWithEhVals shouldn't have extra
     * hours values, the extra hours values are set to 0. Checking if a
     * ConcreteStockWithEhVals should have extra hours values or not can
     * therefore be done by checking if the extra hours price is equal to 0.
     *
     * @return The live change percent
     */
    @Override
    public final double getLiveChangePercent() {
        return ehPrice == 0 ? getChangePercent() : ehChangePercent;
    }

    /**
     * @return The sum of the change percent from the premarket hours, the open
     * hours, and the after hours of the live or most recent trading day.
     */
    @Override
    public final double getNetChangePercent() {
        if (getState() == State.AFTER_HOURS) {
            return getChangePercent() + ehChangePercent;
        } else if (getState() == State.PREMARKET) {
            return ehChangePercent;
        } else {
            return getChangePercent();
        }
    }

    /**
     * @return A seven element string array containing the {@link
     * StockInHomeActivity}'s {@link Stock.State}, price, change point, change
     * percent, extra hours price, extra hours change point, and extra hours
     * change percent.
     */
    @Override
    public String[] getDataAsArray() {
        String[] data = new String[7];
        data[0] = getState().toString();
        data[1] = String.valueOf(getPrice());
        data[2] = String.valueOf(getChangePoint());
        data[3] = String.valueOf(getChangePercent());
        data[4] = String.valueOf(ehPrice);
        data[5] = String.valueOf(ehChangePoint);
        data[6] = String.valueOf(ehChangePercent);
        return data;
    }

    /**
     * @return The price in extra hours trading. This is the live price.
     */
    @Override
    public final double getExtraHoursPrice() {
        return ehPrice;
    }

    @Override
    public final void setExtraHoursPrice(final double extraHoursPrice) {
        ehPrice = extraHoursPrice;
    }

    /**
     * @return The change point that has occurred during extra hours trading
     */
    @Override
    public final double getExtraHoursChangePoint() {
        return ehChangePoint;
    }

    @Override
    public final void setExtraHoursChangePoint(final double extraHoursChangePoint) {
        ehChangePoint = extraHoursChangePoint;
    }

    /**
     * @return The change percent that has occurred during extra hours trading
     */
    @Override
    public final double getExtraHoursChangePercent() {
        return ehChangePercent;
    }

    @Override
    public final void setExtraHoursChangePercent(final double extraHoursChangePercent) {
        ehChangePercent = extraHoursChangePercent;
    }

}
