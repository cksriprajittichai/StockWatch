package com.sienga.stockwatch.listeners;

import com.sienga.stockwatch.stocks.StockInHomeActivity;


public interface FindStockTaskListener {

    void onFindStockTaskCompleted(final int status, final String searchTicker,
                                  final StockInHomeActivity stock);

}
