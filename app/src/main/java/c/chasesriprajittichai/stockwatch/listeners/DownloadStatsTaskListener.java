package c.chasesriprajittichai.stockwatch.listeners;

import java.util.Set;

import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;


public interface DownloadStatsTaskListener {

    void onDownloadStatsTaskCompleted(final int status,
                                      final Set<AdvancedStock.Stat> missingStats);

}
