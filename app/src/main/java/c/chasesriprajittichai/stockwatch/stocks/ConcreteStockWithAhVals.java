package c.chasesriprajittichai.stockwatch.stocks;

/**
 * Regarding the recycler view in {@link
 * c.chasesriprajittichai.stockwatch.HomeActivity}, operations on the recycler
 * view will be much faster if the recycler view only contains one type
 * of object. If an element at an index in the recycler view changes type,
 * expensive operations must be done to account for it. For this reason, the
 * recycler view in HomeActivity only uses ConcreteStockWithAhVals,
 * rather than using both {@link ConcreteStock}) and {@link
 * ConcreteStockWithAhVals}. Additionally, ConcreteStockList has been converted
 * to {@link ConcreteStockWithAhValsList} because of this.
 */
public class ConcreteStockWithAhVals
        extends ConcreteStock
        implements Stock, StockWithAhVals, StockInHomeActivity {

    // Price in after hours; live price
    private double ahPrice;

    // The change point that has occurred during after hours
    private double ahChangePoint;

    // The change percent that has occurred during after hours
    private double ahChangePercent;

    public ConcreteStockWithAhVals(final State state, final String ticker,
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

    /**
     * Because this is the only non-AdvancedStock used in HomeActivity, this
     * class must be able to represent stocks that should have after hours
     * values and stocks that shouldn't. In the code in HomeActivity and
     * MultiStockRequest, if a ConcreteStockWithAhVals shouldn't have after
     * hours values, the after hours values are set to 0. Checking if a
     * ConcreteStockWithAhVals should have after hours values or not can
     * therefore be done by checking if the after hours price is equal to 0.
     *
     * @return The live price
     */
    @Override
    public final double getLivePrice() {
        return ahPrice == 0 ? getPrice() : ahPrice;
    }

    /**
     * Because this is the only non-AdvancedStock used in HomeActivity, this
     * class must be able to represent stocks that should have after hours
     * values and stocks that shouldn't. In the code in HomeActivity and
     * MultiStockRequest, if a ConcreteStockWithAhVals shouldn't have after
     * hours values, the after hours values are set to 0. Checking if a
     * ConcreteStockWithAhVals should have after hours values or not can
     * therefore be done by checking if the after hours price is equal to 0.
     *
     * @return The live change point
     */
    @Override
    public final double getLiveChangePoint() {
        return ahPrice == 0 ? getChangePoint() : ahChangePoint;
    }

    /**
     * Because this is the only non-AdvancedStock used in HomeActivity, this
     * class must be able to represent stocks that should have after hours
     * values and stocks that shouldn't. In the code in HomeActivity and
     * MultiStockRequest, if a ConcreteStockWithAhVals shouldn't have after
     * hours values, the after hours values are set to 0. Checking if a
     * ConcreteStockWithAhVals should have after hours values or not can
     * therefore be done by checking if the after hours price is equal to 0.
     *
     * @return The live change percent
     */
    @Override
    public final double getLiveChangePercent() {
        return ahPrice == 0 ? getChangePercent() : ahChangePercent;
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
    public String[] getDataAsArray() {
        String[] data = new String[7];
        data[0] = getState().toString();
        data[1] = String.valueOf(getPrice());
        data[2] = String.valueOf(getChangePoint());
        data[3] = String.valueOf(getChangePercent());
        data[4] = String.valueOf(ahPrice);
        data[5] = String.valueOf(ahChangePoint);
        data[6] = String.valueOf(ahChangePercent);
        return data;
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
