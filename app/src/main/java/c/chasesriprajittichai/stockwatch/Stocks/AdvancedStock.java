package c.chasesriprajittichai.stockwatch.Stocks;

import java.util.ArrayList;

public class AdvancedStock extends BasicStock {

    private String mname;
    private ArrayList<Double> myData;

    public AdvancedStock(State state, String ticker, String name, double price, double changePoint,
                         double changePercent, ArrayList<Double> yData) {
        super(state, ticker, price, changePoint, changePercent);
        this.mname = name;

        this.myData = yData;
    }

    public String getName() {
        return mname;
    }

    public void setName(String name) {
        this.mname = name;
    }

    public ArrayList<Double> getyData() {
        return myData;
    }

    public void setyData(ArrayList<Double> yData) {
        this.myData = yData;
    }

}
