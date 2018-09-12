package com.sienga.stockwatch.listeners;

import java.util.Set;

import com.sienga.stockwatch.stocks.AdvancedStock;


public interface DownloadChartsTaskListener {

    void onDownloadChartsTaskCompleted(final int status,
                                       final Set<AdvancedStock.ChartPeriod> missingChartPeriods);

}
