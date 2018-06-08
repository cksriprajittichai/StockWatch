package c.chasesriprajittichai.stockwatch.listeners;

public interface FindStockTaskListener {
    void onFindStockTaskCompleted(String ticker, boolean stockExists);
}
