package c.chasesriprajittichai.stockwatch.listeners;

public interface FindStockTaskListener {

    void onFindStockTaskCompleted(final String ticker, final String name, final boolean stockExists);

}
