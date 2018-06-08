package c.chasesriprajittichai.stockwatch.stocks;

import java.util.ArrayList;

public class AdvancedStock extends BasicStock {

    private String mname;
    private ArrayList<Double> myData;

    public AdvancedStock(State state, String ticker, String name, double price, double changePoint,
                         double changePercent, ArrayList<Double> yData) {
        super(state, ticker, price, changePoint, changePercent);
        mname = name;

        myData = yData;
    }

    public String getName() {
        return mname;
    }

    public void setName(String name) {
        mname = name;
    }

    public ArrayList<Double> getyData() {
        return myData;
    }

    public void setyData(ArrayList<Double> yData) {
        myData = yData;
    }

}
