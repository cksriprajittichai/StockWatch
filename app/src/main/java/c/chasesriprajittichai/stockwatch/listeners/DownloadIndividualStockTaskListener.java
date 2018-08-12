package c.chasesriprajittichai.stockwatch.listeners;

import java.util.Set;

import c.chasesriprajittichai.stockwatch.IndividualStockActivity;
import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;

public interface DownloadIndividualStockTaskListener {

    void onDownloadIndividualStockTaskCompleted(final AdvancedStock stock,
                                                final Set<IndividualStockActivity.Stat> missingStats,
                                                final Set<AdvancedStock.ChartPeriod> missingChartPeriods);

}
