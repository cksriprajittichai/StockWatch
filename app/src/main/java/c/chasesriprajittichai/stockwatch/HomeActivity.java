package c.chasesriprajittichai.stockwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import c.chasesriprajittichai.stockwatch.listeners.FindStockTaskListener;
import c.chasesriprajittichai.stockwatch.recyclerview.RecyclerAdapter;
import c.chasesriprajittichai.stockwatch.recyclerview.RecyclerDivider;
import c.chasesriprajittichai.stockwatch.recyclerview.StockSwipeAndDragCallback;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStock;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithAhVals;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithAhValsList;
import c.chasesriprajittichai.stockwatch.stocks.Stock;
import c.chasesriprajittichai.stockwatch.stocks.StockInHomeActivity;

import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.ERROR;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.PREMARKET;
import static java.lang.Double.parseDouble;


public final class HomeActivity extends AppCompatActivity implements FindStockTaskListener,
        Response.Listener<ConcreteStockWithAhValsList>, Response.ErrorListener {


    private static class FindStockTask extends AsyncTask<Void, Integer, StockInHomeActivity> {

        private final String searchTicker;
        private StockInHomeActivity stock;
        private final WeakReference<FindStockTaskListener> completionListener;

        private FindStockTask(final String searchTicker, final FindStockTaskListener completionListener) {
            this.searchTicker = searchTicker;
            this.completionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected StockInHomeActivity doInBackground(final Void... params) {
            final String URL = "https://quotes.wsj.com/" + searchTicker;

            final boolean stockExists;
            try {
                final Document doc = Jsoup.connect(URL).get();

                final Element contentFrame = doc.selectFirst(
                        "html > body > div.pageFrame > div.contentFrame");

                // If the stock's page is found on WSJ, this element does not exist
                final Element flagElmnt = contentFrame.selectFirst(
                        "div[class$=notfound_header module]");

                stockExists = (flagElmnt == null);

                if (stockExists) {
                    final String name = contentFrame.selectFirst(
                            "span.companyName").ownText();

                    final Element module2 = contentFrame.selectFirst(
                            ":root > section[class$=section_1] > div.zonedModule[data-module-id=2]");
                    final Element mainData = module2.selectFirst(
                            "ul[class$=info_main]");

                    // Remove ',' or '%' that could be in strings
                    final double price = parseDouble(mainData.selectFirst(
                            ":root > li[class$=quote] > span.curr_price > " +
                                    "span > span#quote_val").ownText().replaceAll("[^0-9.]+", ""));
                    final Elements diffs = mainData.select(
                            ":root > li[class$=diff] > span > span");
                    final double changePoint = parseDouble(
                            diffs.get(0).ownText().replaceAll("[^0-9.-]+", ""));
                    final double changePercent = parseDouble(
                            diffs.get(1).ownText().replaceAll("[^0-9.-]+", ""));

                    final Element subData = mainData.nextElementSibling();
                    boolean stockHasAhVals = subData.className().endsWith("info_sub");

                    final Stock.State state;
                    final String stateStr;
                    if (stockHasAhVals) {
                        final double ahPrice, ahChangePoint, ahChangePercent;
                        // Remove ',' or '%' that could be in strings
                        ahPrice = parseDouble(subData.selectFirst(
                                "span#ms_quote_val").ownText().replaceAll("[^0-9.]+", ""));
                        final Elements ah_diffs = subData.select(
                                "span[id] > span");
                        ahChangePoint = parseDouble(ah_diffs.get(0).ownText().replaceAll("[^0-9.-]+", ""));
                        ahChangePercent = parseDouble(ah_diffs.get(1).ownText().replaceAll("[^0-9.-]+", ""));

                        stateStr = subData.selectFirst("span").ownText();
                        state = stateStr.equals("AFTER HOURS") ? AFTER_HOURS : PREMARKET;

                        stock = new ConcreteStockWithAhVals(state, searchTicker, name,
                                price, changePoint, changePercent,
                                ahPrice, ahChangePoint, ahChangePercent);
                    } else {
                        stateStr = mainData.selectFirst("span.timestamp_label").ownText();
                        state = stateStr.equals("REAL TIME") ? OPEN : CLOSED;

                        stock = new ConcreteStock(state, searchTicker, name,
                                price, changePoint, changePercent);
                    }
                }
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
            }

            return stock;
        }

        @Override
        protected void onPostExecute(final StockInHomeActivity stock) {
            completionListener.get().onFindStockTaskCompleted(searchTicker, stock);
        }

    }


    @BindView(R.id.recyclerView_home) RecyclerView rv;

    private ConcreteStockWithAhValsList stocks;
    private RecyclerAdapter rvAdapter;
    private SearchView searchView;
    private SharedPreferences prefs;
    private RequestQueue requestQueue;
    private Timer timer;

    // Maps tickers to indexes in stocks
    private final Map<String, Integer> tickerToIndexMap = new HashMap<>();

    /* Called from FindStockTask.onPostExecute(). */
    @Override
    public void onFindStockTaskCompleted(final String searchTicker, final StockInHomeActivity stock) {
        if (stock != null) {
            // Go to individual stock activity
            final Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", stock.getTicker());
            intent.putExtra("Name", stock.getName());
            intent.putExtra("Data", stock.getDataAsArray());

            // Equivalent to checking if searchTicker is in stocks
            intent.putExtra("Is in favorites", tickerToIndexMap.containsKey(stock.getTicker()));
            startActivity(intent);
        } else {
            searchView.setQuery("", false);
            Toast.makeText(HomeActivity.this,
                    searchTicker + " couldn't be found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Stock Watch");
        ButterKnife.bind(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        requestQueue = Volley.newRequestQueue(this);
        stocks = new ConcreteStockWithAhValsList();

        /* Starter kit */
//        fillPreferencesWithRandomStocks(0);

        final String[] tickerArr = prefs.getString("Tickers TSV", "").split("\t");
        final String[] nameArr = prefs.getString("Names TSV", "").split("\t");
        final String[] dataArr = prefs.getString("Data TSV", "").split("\t");

        /* If there are stocks in favorites, initialize recycler view to show
         * tickers with the previous saved data. */
        if (!tickerArr[0].isEmpty()) {
            stocks.ensureCapacity(tickerArr.length);
            ConcreteStockWithAhVals curStock;
            String curTicker, curName;
            Stock.State curState;
            double curPrice, curChangePoint, curChangePercent,
                    curAhPrice, curAhChangePoint, curAhChangePercent;
            for (int tickerNdx = 0, dataNdx = 0; tickerNdx < tickerArr.length; tickerNdx++, dataNdx += 7) {
                // nameNdx is the same as tickerNdx (1 searchTicker for 1 name, unlike Data TSV)
                curTicker = tickerArr[tickerNdx];
                curName = nameArr[tickerNdx];

                curState = Util.stringToStateMap.get(dataArr[dataNdx]);
                if (curState == ERROR) {
                    Log.e("ErrorStateInHomeActivity", String.format(
                            "Stock with state equal to ERROR in HomeActivity.%n" +
                                    "Ticker: %s", curTicker));
                    // Do not add this error stock to stocks or to tickerToIndexMap
                    continue;
                }

                curPrice = parseDouble(dataArr[dataNdx + 1]);
                curChangePoint = parseDouble(dataArr[dataNdx + 2]);
                curChangePercent = parseDouble(dataArr[dataNdx + 3]);
                curAhPrice = parseDouble(dataArr[dataNdx + 4]);
                curAhChangePoint = parseDouble(dataArr[dataNdx + 5]);
                curAhChangePercent = parseDouble(dataArr[dataNdx + 6]);

                curStock = new ConcreteStockWithAhVals(
                        curState, curTicker, curName,
                        curPrice, curChangePoint, curChangePercent,
                        curAhPrice, curAhChangePoint, curAhChangePercent);

                // Fill stocks and tickerToIndexMap
                stocks.add(curStock);
                tickerToIndexMap.put(curTicker, tickerNdx);
            }
        }

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new RecyclerDivider(this));
        rvAdapter = new RecyclerAdapter(stocks, (ConcreteStockWithAhVals stock) -> {
            // Go to individual stock activity
            final Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", stock.getTicker());
            intent.putExtra("Name", stock.getName());
            intent.putExtra("Data", stock.getDataAsArray());

            // Equivalent to checking if searchTicker is in stocks
            intent.putExtra("Is in favorites", tickerToIndexMap.containsKey(stock.getTicker()));
            startActivity(intent);
        });
        rv.setAdapter(rvAdapter);
        // Init swipe to delete for rv.
        final StockSwipeAndDragCallback stockSwipeAndDragCallback =
                new StockSwipeAndDragCallback(this, rvAdapter, stocks, tickerToIndexMap);
        new ItemTouchHelper(stockSwipeAndDragCallback).attachToRecyclerView(rv);

        checkForUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If there are stocks in favorites, update stocks and rv
        timer = new Timer();
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!stocks.isEmpty()) {
                    updateStocks();
                }
            }
        };
        // Run every 10 seconds, starting immediately
        timer.schedule(timerTask, 0, 10000);

        checkForCrashes();
    }

    @Override
    protected void onPause() {
        super.onPause();

        /* Stop updating. This ruins timer and mtimerTask, so they must be
         * re-initialized to be used again. */
        timer.cancel();

        prefs.edit().putString("Tickers TSV", stocks.getStockTickersAsTSV()).apply();
        prefs.edit().putString("Names TSV", stocks.getStockNamesAsTSV()).apply();
        prefs.edit().putString("Data TSV", stocks.getStockDataAsTSV()).apply();

        unregisterManagers();
    }

    @Override
    public void onBackPressed() {
        final Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        requestQueue.cancelAll(this);
    }

    /**
     * Partitions stocks in stocks into sets of maximum size 10. The maximum
     * size of each partition is 10 because the Market Watch
     * multiple-stock-website only supports displaying up to 10 stocks. The
     * tickers from each partition's stocks are used to build a URL for the
     * Market Watch multiple-stock-website. For each partition, a
     * MultiStockRequest is created using the URL and the references to the
     * stocks represented in the URL. The MultiStockRequest is then added to
     * requestQueue.
     */
    private void updateStocks() {
        /* During this function's lifetime, the user could swipe-delete a stock.
         * Using the original stocks in stocks allows us to not worry about the
         * consequences of a stock being removed from stocks. As a result, this
         * function, as well as MultiStockRequest could possibly edit stocks
         * that have been removed from stocks. HomeActivity.onResponse() handles
         * this by ensuring that stocks contains a stock before updating the
         * UI. */
        final ConcreteStockWithAhValsList stocksToUpdate = new ConcreteStockWithAhValsList(stocks);
        final int numStocksTotal = stocksToUpdate.size();
        int numStocksUpdated = 0;

        // URL form: <base URL><searchTicker 1>,<searchTicker 2>,<searchTicker 3>,<searchTicker n>
        final String BASE_URL_MULTI = "https://www.marketwatch.com/investing/multi?tickers=";
        /* Up to 10 stocks are shown in the MarketWatch view multiple stocks
         * website. The first 10 tickers listed in the URL are shown. Appending
         * more than 10 tickers onto the URL has no effect on the website - the
         * first 10 tickers will be shown. */
        final StringBuilder tickersPartUrl = new StringBuilder(50); // Approximate size

        ConcreteStockWithAhValsList stocksToUpdateThisIteration;
        int numStocksToUpdateThisIteration;
        while (numStocksUpdated < numStocksTotal) {
            // Ex: if we've already updated 30 / 37 stocks, finish only 7 on the last iteration
            if (numStocksTotal - numStocksUpdated >= 10) {
                numStocksToUpdateThisIteration = 10;
            } else {
                numStocksToUpdateThisIteration = numStocksTotal - numStocksUpdated;
            }

            stocksToUpdateThisIteration = new ConcreteStockWithAhValsList(
                    stocksToUpdate.subList(
                            numStocksUpdated,
                            numStocksUpdated + numStocksToUpdateThisIteration));

            // Append tickers for stocks that will be updated in this iteration
            for (final Stock s : stocksToUpdateThisIteration) {
                tickersPartUrl.append(s.getTicker());
                tickersPartUrl.append(',');
            }
            tickersPartUrl.deleteCharAt(tickersPartUrl.length() - 1); // Delete extra comma

            // Send stocks that will be updated and their URL to the MultiStockRequest
            requestQueue.add(new MultiStockRequest(BASE_URL_MULTI + tickersPartUrl.toString(),
                    stocksToUpdateThisIteration, this, this));

            numStocksUpdated += numStocksToUpdateThisIteration;
            tickersPartUrl.setLength(0); // Clear tickers part of the URL
        }
    }

    @Override
    public void onResponse(final ConcreteStockWithAhValsList updatedStocks) {
        for (final Stock s : updatedStocks) {
            /* Updating the recycler view while dragging disrupts the dragging,
             * causing the currently dragged item to fall over whatever position
             * it is hovers over.
             * The user could have swipe-deleted curStock in the time that the
             * MultiStockRequest was executing. If so, tickerToIndexMap does
             * not contain curTicker. */
            if (!rvAdapter.isDragging() && tickerToIndexMap.containsKey(s.getTicker())) {
                rvAdapter.notifyItemChanged(tickerToIndexMap.get(s.getTicker()));
            }
        }
    }

    @Override
    public void onErrorResponse(final VolleyError error) {
        if (error.getLocalizedMessage() != null && !error.getLocalizedMessage().isEmpty()) {
            Log.e("VolleyError", error.getLocalizedMessage());
        } else {
            Log.e("VolleyError",
                    "VolleyError thrown in HomeActivity.onErrorResponse, but error is null.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_activity, menu);

        searchView = (SearchView) menu.findItem(R.id.menuItem_search_home).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                final String ticker = query.trim().toUpperCase();

                /* Valid tickers are between [1,5] characters long, with all
                 * characters being either digits, letters, or '.'. */
                final boolean isValidTicker = ticker.matches("[0-9A-Z.]{1,6}");

                if (isValidTicker) {
                    FindStockTask task = new FindStockTask(ticker, HomeActivity.this);
                    task.execute();

                    /* Prevent spamming of submit button. This also prevents
                     * multiple FindStockTasks from being executed at the same
                     * time. Similar effect as disabling the SearchView submit
                     * button. */
                    searchView.clearFocus();
                } else {
                    Toast.makeText(HomeActivity.this,
                            ticker + " is an invalid symbol", Toast.LENGTH_SHORT).show();
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        /* All of these list transformations change the indexing of the stocks
         * in stocks. Therefore, each of these list transformations must call
         * updateTickerToIndexMap(). */
        switch (item.getItemId()) {
            case R.id.menuItem_sortAlphabetically_home:
                final Comparator<Stock> tickerComparator =
                        Comparator.comparing(Stock::getTicker);
                stocks.sort(tickerComparator);
                updateTickerToIndexMap();
                rvAdapter.notifyItemRangeChanged(0, rvAdapter.getItemCount());
                return true;
            case R.id.menuItem_sortByPrice_home:
                // Sort by decreasing price
                final Comparator<Stock> descPriceComparator =
                        Comparator.comparingDouble(Stock::getPrice).reversed();
                stocks.sort(descPriceComparator);
                updateTickerToIndexMap();
                rvAdapter.notifyItemRangeChanged(0, rvAdapter.getItemCount());
                return true;
            case R.id.menuItem_sortByChangePercent_home:
                // Sort by decreasing magnitude of change percent
                stocks.sort((Stock a, Stock b) -> {
                    // Ignore sign, compare change percents by magnitude
                    return Double.compare(Math.abs(b.getChangePercent()), Math.abs(a.getChangePercent()));
                });
                updateTickerToIndexMap();
                rvAdapter.notifyItemRangeChanged(0, rvAdapter.getItemCount());
                return true;
            case R.id.menuItem_sortByState_home:
                final Comparator<Stock> stateComparator =
                        Comparator.comparing(Stock::getState);
                stocks.sort(stateComparator);
                updateTickerToIndexMap();
                rvAdapter.notifyItemRangeChanged(0, rvAdapter.getItemCount());
                return true;
            case R.id.menuItem_shuffle_home:
                Collections.shuffle(stocks);
                updateTickerToIndexMap();
                rvAdapter.notifyItemRangeChanged(0, rvAdapter.getItemCount());
                return true;
            case R.id.menuItem_flipList_home:
                Collections.reverse(stocks);
                updateTickerToIndexMap();
                rvAdapter.notifyItemRangeChanged(0, rvAdapter.getItemCount());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This function should be called when a stock is removed by being swiped
     * away. This function updates the indexes in stocks that a searchTicker in
     * tickerToIndexMap maps to. After a swipe deletion, the stocks in range
     * [index of stock removed, size of stocks] of stocks need to be updated.
     * The stocks in this range are now at their previous index - 1 in stocks.
     * This function does not remove any mappings from tickerToIndexMap.
     *
     * @param lastRemovedIndex The index of stock most recently removed
     */
    public synchronized void updateTickerToIndexMap(final int lastRemovedIndex) {
        if (lastRemovedIndex >= stocks.size()) {
            // If the last removed stock was the last stock in stocks
            return;
        }

        for (int i = lastRemovedIndex; i < stocks.size(); i++) {
            tickerToIndexMap.put(stocks.get(i).getTicker(), i);
        }
    }

    /**
     * Updates the indexes in stocks that a searchTicker in tickerToIndexMap maps to. This
     * function clears tickerToIndexMap and reconstructs it with the current stock
     * indexing of stocks. This should be called anytime after the indexing of stocks
     * in stocks changes. This is equivalent to calling
     * {@link #updateTickerToIndexMap(int)} and passing in 0.
     */
    public synchronized void updateTickerToIndexMap() {
        tickerToIndexMap.clear();
        for (int i = 0; i < stocks.size(); i++) {
            tickerToIndexMap.put(stocks.get(i).getTicker(), i);
        }
    }

    /**
     * Fills Tickers TSV with tickers from the NASDAQ and sets all of Data TSV's
     * numeric values to -1 and sets Data TSV's {@link Stock.State} values to
     * {@link Stock.State#CLOSED} . Fills Names TSV with "[searchTicker] name",
     * because we don't know the names of the stocks, only the tickers. The
     * largest number of stocks that can be added is the number of companies in
     * the NASDAQ.
     *
     * @param size The number of stocks to put in preferences
     */
    private void fillPreferencesWithRandomStocks(int size) {
        final String tickersStr = getString(R.string.nasdaqTickers);

        final String[] tickerArr = tickersStr.split(",");
        final String[] nameArr = new String[tickerArr.length];
        for (int i = 0; i < nameArr.length; i++) {
            nameArr[i] = tickerArr[i] + " name";
        }
        boolean usingMaxSize = false;
        if (size >= tickerArr.length) {
            size = tickerArr.length;
            usingMaxSize = true;
        }

        if (usingMaxSize) {
            prefs.edit().putString("Tickers TSV", TextUtils.join("\t", tickerArr)).apply();
            prefs.edit().putString("Names TSV", TextUtils.join("\t", nameArr)).apply();
        } else {
            final String[] subTickerArr = new String[size];
            System.arraycopy(tickerArr, 0, subTickerArr, 0, size);
            prefs.edit().putString("Tickers TSV", TextUtils.join("\t", subTickerArr)).apply();
            final String[] subNameArr = new String[size];
            System.arraycopy(nameArr, 0, subNameArr, 0, size);
            prefs.edit().putString("Names TSV", TextUtils.join("\t", subNameArr)).apply();
        }

        final String[] dataArr = new String[7 * size];
        for (int i = 0; i < size * 7; i += 7) {
            dataArr[i] = Stock.State.CLOSED.toString();
            dataArr[i + 1] = "-1";
            dataArr[i + 2] = "-1";
            dataArr[i + 3] = "-1";
            dataArr[i + 4] = "-1";
            dataArr[i + 5] = "-1";
            dataArr[i + 6] = "-1";
        }
        prefs.edit().putString("Data TSV", TextUtils.join("\t", dataArr)).apply();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterManagers();
    }

    private void checkForCrashes() {
        CrashManager.register(this);
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this);
    }

    private void unregisterManagers() {
        UpdateManager.unregister();
    }

}
