package c.chasesriprajittichai.stockwatch.AsyncTaskListeners;

import c.chasesriprajittichai.stockwatch.BasicStock;

public interface DownloadIndividualStockTask {
    void onDownloadIndividualStockTaskCompleted(BasicStock stock);
}
