package c.chasesriprajittichai.stockwatch;

import com.robinhood.spark.SparkAdapter;

import java.util.ArrayList;

public class SparkViewAdapter extends SparkAdapter {

    private ArrayList<Double> myData;

    public SparkViewAdapter(ArrayList<Double> yData) {
        this.myData = yData;
    }

    @Override
    public int getCount() {
        return myData.size();
    }

    @Override
    public Object getItem(int index) {
        return myData.get(index);
    }

    @Override
    public float getY(int index) {
        return (float) myData.get(index).doubleValue();
    }

    public double getyData(int index) {
        return myData.get(index);
    }

    public ArrayList<Double> getyData() {
        return myData;
    }

    /* Does not call notifyDataSetChanged(). */
    public void setyData(int index, double data) {
        myData.set(index, data);
    }

    /* Does not call notifyDataSetChanged(). */
    public void setyData(ArrayList<Double> yData) {
        this.myData = yData;
    }

}
