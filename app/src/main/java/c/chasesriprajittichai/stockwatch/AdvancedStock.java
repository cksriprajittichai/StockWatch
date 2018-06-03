package c.chasesriprajittichai.stockwatch;

import java.util.ArrayList;

public class AdvancedStock extends BasicStock {

    private String name;
    private ArrayList<Double> yData;

    public AdvancedStock(State state, String ticker, String name, double price, double changePoint,
                         double changePercent, ArrayList<Double> yData) {
        super(state, ticker, price, changePoint, changePercent);
        this.name = name;

        this.yData = yData;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Double> getyData() {
        return yData;
    }

    public void setyData(ArrayList<Double> yData) {
        this.yData = yData;
    }

}
