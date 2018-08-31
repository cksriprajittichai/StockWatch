package c.chasesriprajittichai.stockwatch.listeners;

import java.util.Set;

import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;


public interface DownloadChartsTaskListener {

    void onDownloadChartsTaskCompleted(final int status,
                                       final Set<AdvancedStock.ChartPeriod> missingChartPeriods);

}
