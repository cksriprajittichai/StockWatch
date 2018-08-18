package c.chasesriprajittichai.stockwatch.listeners;

import java.util.Set;

import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;


public interface DownloadChartTaskListener {

    void onDownloadChartTaskCompleted(final int status,
                                      final Set<AdvancedStock.ChartPeriod> missingChartPeriods);

}
