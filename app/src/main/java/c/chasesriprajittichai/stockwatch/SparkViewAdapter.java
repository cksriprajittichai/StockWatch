package c.chasesriprajittichai.stockwatch;

import com.robinhood.spark.SparkAdapter;

import java.util.ArrayList;

public class SparkViewAdapter extends SparkAdapter {

    private ArrayList<Double> yData;

    public SparkViewAdapter(ArrayList<Double> yData) {
        this.yData = yData;
    }

    @Override
    public int getCount() {
        return yData.size();
    }

    @Override
    public Object getItem(int index) {
        return yData.get(index);
    }

    @Override
    public float getY(int index) {
        return (float) yData.get(index).doubleValue();
    }

    public double getyData(int index) {
        return yData.get(index);
    }

    public ArrayList<Double> getyData() {
        return yData;
    }

    /* Does not call notifyDataSetChanged(). */
    public void setyData(int index, double data) {
        yData.set(index, data);
    }

    /* Does not call notifyDataSetChanged(). */
    public void setyData(ArrayList<Double> yData) {
        this.yData = yData;
    }

}
