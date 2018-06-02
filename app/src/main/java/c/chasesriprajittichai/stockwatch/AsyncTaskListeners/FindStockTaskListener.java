package c.chasesriprajittichai.stockwatch.AsyncTaskListeners;

public interface FindStockTaskListener {
    void onFindStockTaskCompleted(String ticker, boolean stockExists);
}
