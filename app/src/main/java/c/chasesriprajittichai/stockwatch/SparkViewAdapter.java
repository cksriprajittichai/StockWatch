package c.chasesriprajittichai.stockwatch;

import com.robinhood.spark.SparkAdapter;

import java.util.ArrayList;
import java.util.List;

import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;


public final class SparkViewAdapter extends SparkAdapter {

    private List<Double> prices;
    private List<String> dates;
    private AdvancedStock.ChartPeriod chartPeriod = AdvancedStock.ChartPeriod.ONE_DAY; // Initial period

    SparkViewAdapter() {
        prices = new ArrayList<>();
        dates = new ArrayList<>();
    }

    SparkViewAdapter(final List<Double> yData, final List<String> dates) {
        prices = yData;
        this.dates = dates;
    }

    @Override
    public int getCount() {
        return prices.size();
    }

    @Override
    public Object getItem(final int index) {
        return prices.get(index);
    }

    @Override
    public float getY(final int index) {
        return (float) prices.get(index).doubleValue();
    }

    public String getDate(final int index) {
        return dates.get(index);
    }

    /* Does not call notifyDataSetChanged(). */
    public void setyData(final List<Double> yData) {
        prices = yData;
    }

    /* Does not call notifyDataSetChanged(). */
    public void setDates(final List<String> dates) {
        this.dates = dates;
    }

    public AdvancedStock.ChartPeriod getChartPeriod() {
        return chartPeriod;
    }

    public void setChartPeriod(final AdvancedStock.ChartPeriod chartPeriod) {
        this.chartPeriod = chartPeriod;
    }

}
