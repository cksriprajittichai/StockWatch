package com.sienga.stockwatch.listeners;

import java.util.Set;

import com.sienga.stockwatch.stocks.AdvancedStock;


public interface DownloadStatsTaskListener {

    void onDownloadStatsTaskCompleted(final int status,
                                      final Set<AdvancedStock.Stat> missingStats);

}
