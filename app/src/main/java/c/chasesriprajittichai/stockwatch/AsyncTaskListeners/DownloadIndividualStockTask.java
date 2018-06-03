package c.chasesriprajittichai.stockwatch.AsyncTaskListeners;

import c.chasesriprajittichai.stockwatch.AdvancedStock;

public interface DownloadIndividualStockTask {
    void onDownloadIndividualStockTaskCompleted(AdvancedStock stock);
}
