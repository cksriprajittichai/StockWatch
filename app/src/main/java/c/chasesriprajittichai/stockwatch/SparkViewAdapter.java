package c.chasesriprajittichai.stockwatch;

import com.robinhood.spark.SparkAdapter;

import java.util.ArrayList;
import java.util.List;

import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;


public final class SparkViewAdapter extends SparkAdapter {

    private List<Double> prices;
    private List<String> dates;
    private AdvancedStock.ChartPeriod chartPeriod = null;

    SparkViewAdapter() {
        prices = new ArrayList<>();
        dates = new ArrayList<>();
    }

    /**
     * @return the number of points to be drawn
     */
    @Override
    public int getCount() {
        return prices.size();
    }

    /**
     * @return the object at the given index
     */
    @Override
    public Object getItem(final int index) {
        return prices.get(index);
    }

    /**
     * @return the float representation of the Y value of the point at the given
     * index
     */
    @Override
    public float getY(final int index) {
        return (float) prices.get(index).doubleValue();
    }

    public float getPrice(final int index) {
        return getY(index);
    }

    public String getDate(final int index) {
        return dates.get(index);
    }

    public List<Double> getPrices() {
        return prices;
    }

    /**
     * This function does not call {@link #notifyDataSetChanged()}.
     *
     * @param yData To set prices to
     */
    public void setPrices(final List<Double> yData) {
        prices = yData;
    }

    public List<String> getDates() {
        return dates;
    }

    /**
     * This function does not call {@link #notifyDataSetChanged()}.
     *
     * @param dates To set dates to
     */
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
