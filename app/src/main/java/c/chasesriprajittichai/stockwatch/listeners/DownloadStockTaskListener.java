package c.chasesriprajittichai.stockwatch.listeners;

import java.util.Set;

import c.chasesriprajittichai.stockwatch.IndividualStockActivity;
import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;


public interface DownloadStockTaskListener {
    // Download AdvancedStock task listener. IndividualStockActivity implements this.

    void onDownloadStockTaskCompleted(final AdvancedStock stock,
                                      final Set<IndividualStockActivity.Stat> missingStats,
                                      final Set<AdvancedStock.ChartPeriod> missingChartPeriods);

}
