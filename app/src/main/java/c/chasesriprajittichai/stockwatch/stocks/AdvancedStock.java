package c.chasesriprajittichai.stockwatch.stocks;

import java.util.ArrayList;
import java.util.Collections;

public class AdvancedStock extends BasicStock {

    private final String mname;
    private final String mdescription;
    private final ArrayList<Double> myData;

    public AdvancedStock(final State state, final String ticker, final String name,
                         final double price, final double changePoint, final double changePercent,
                         final String description, final ArrayList<Double> yData) {
        super(state, ticker, price, changePoint, changePercent);
        mname = name;
        mdescription = description;
        myData = yData;
    }

    public String getName() {
        return mname;
    }

    public String getDescription() {
        return mdescription;
    }

    public ArrayList<Double> getyData() {
        return myData;
    }

    public void setyData(final ArrayList<Double> yData) {
        myData.clear();
        Collections.copy(myData, yData);
    }

}
