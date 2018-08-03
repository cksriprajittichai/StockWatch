package c.chasesriprajittichai.stockwatch;

import com.robinhood.spark.SparkAdapter;

import java.util.ArrayList;
import java.util.List;

import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;

public final class SparkViewAdapter extends SparkAdapter {

    private List<Double> myData;
    private List<String> mdates;
    private AdvancedStock.ChartPeriod mchartPeriod = AdvancedStock.ChartPeriod.ONE_DAY; // Initial period

    SparkViewAdapter() {
        myData = new ArrayList<>();
        mdates = new ArrayList<>();
    }

    SparkViewAdapter(final List<Double> yData, final List<String> dates) {
        myData = yData;
        mdates = dates;
    }

    @Override
    public int getCount() {
        return myData.size();
    }

    @Override
    public Object getItem(final int index) {
        return myData.get(index);
    }

    @Override
    public float getY(final int index) {
        return (float) myData.get(index).doubleValue();
    }

    public String getDate(final int index) {
        return mdates.get(index);
    }

    /* Does not call notifyDataSetChanged(). */
    public void setyData(final List<Double> yData) {
        myData = yData;
    }

    /* Does not call notifyDataSetChanged(). */
    public void setDates(final List<String> dates) {
        mdates = dates;
    }

    public AdvancedStock.ChartPeriod getChartPeriod() {
        return mchartPeriod;
    }

    public void setChartPeriod(final AdvancedStock.ChartPeriod chartPeriod) {
        mchartPeriod = chartPeriod;
    }

}
