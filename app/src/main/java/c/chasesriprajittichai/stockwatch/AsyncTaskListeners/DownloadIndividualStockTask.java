package c.chasesriprajittichai.stockwatch.AsyncTaskListeners;

import c.chasesriprajittichai.stockwatch.Stocks.AdvancedStock;

public interface DownloadIndividualStockTask {
    void onDownloadIndividualStockTaskCompleted(AdvancedStock stock);
}
