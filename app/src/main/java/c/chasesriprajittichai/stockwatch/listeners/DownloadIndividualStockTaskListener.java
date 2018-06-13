package c.chasesriprajittichai.stockwatch.listeners;

import java.util.HashSet;

import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;

public interface DownloadIndividualStockTaskListener {
    void onDownloadIndividualStockTaskCompleted(final AdvancedStock stock, final HashSet<String> missingStats);
}
