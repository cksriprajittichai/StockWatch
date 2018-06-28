package c.chasesriprajittichai.stockwatch;

import com.robinhood.spark.SparkAdapter;

import java.util.ArrayList;

import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;

public final class SparkViewAdapter extends SparkAdapter {

    private ArrayList<Double> myData;

    private AdvancedStock.ChartPeriod mchartPeriod = AdvancedStock.ChartPeriod.ONE_DAY; // Initial period

    SparkViewAdapter(final ArrayList<Double> yData) {
        myData = yData;
    }

    @Override
    public int getCount() {
        return myData.size();
    }

    @Override
    public Object getItem(final int index) {
        return myData.get(index);
    }

    /* This function shouldn't be used. Chart data should be taken from the AdvancedStock directly. */
    @Override
    public float getY(final int index) {
        return (float) myData.get(index).doubleValue();
    }

    /* Does not call notifyDataSetChanged(). */
    public void setyData(final ArrayList<Double> yData) {
        myData = yData;
    }

    public AdvancedStock.ChartPeriod getChartPeriod() {
        return mchartPeriod;
    }

    public void setMchartPeriod(final AdvancedStock.ChartPeriod chartPeriod) {
        mchartPeriod = chartPeriod;
    }

}
