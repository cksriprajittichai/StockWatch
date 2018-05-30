package c.chasesriprajittichai.stockwatch;

public interface FindStockTaskListener {

    void onFindStockTaskCompleted(String ticker, boolean stockExists);

}
