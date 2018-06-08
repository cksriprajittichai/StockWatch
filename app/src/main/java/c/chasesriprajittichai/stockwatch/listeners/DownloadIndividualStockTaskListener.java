package c.chasesriprajittichai.stockwatch.listeners;

import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;

public interface DownloadIndividualStockTaskListener {
    void onDownloadIndividualStockTaskCompleted(AdvancedStock stock);
}
