package c.chasesriprajittichai.stockwatch.listeners;

import c.chasesriprajittichai.stockwatch.stocks.StockInHomeActivity;


public interface FindStockTaskListener {

    void onFindStockTaskCompleted(final String searchTicker, final StockInHomeActivity stock);

}
