package c.chasesriprajittichai.stockwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import c.chasesriprajittichai.stockwatch.listeners.FindStockTaskListener;
import c.chasesriprajittichai.stockwatch.recyclerviews.StockRecyclerAdapter;
import c.chasesriprajittichai.stockwatch.recyclerviews.StockRecyclerDivider;
import c.chasesriprajittichai.stockwatch.recyclerviews.StockSwipeAndDragCallback;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStock;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithEhVals;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithEhValsList;
import c.chasesriprajittichai.stockwatch.stocks.Stock;
import c.chasesriprajittichai.stockwatch.stocks.StockInHomeActivity;

import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.ERROR;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.PREMARKET;
import static java.lang.Double.parseDouble;


public final class HomeActivity
        extends AppCompatActivity
        implements FindStockTaskListener,
        Response.Listener<ConcreteStockWithEhValsList>,
        Response.ErrorListener {

    @BindView(R.id.recyclerView_stockRecycler) RecyclerView rv;

    /**
     * This is true if this Activity is being shown. This is set to false in
     * {@link #onPause()}, and set to true in {@link #onResume()}.
     * <p>
     * This is used in {@link
     * #onFindStockTaskCompleted(int, String, StockInHomeActivity)} to check
     * that this Activity is visible before responding to the {@link
     * FindStockTask} (i.e. start a new IndividualStockActivity for the searched
     * Stock, or display a toast saying that a Stock with the searched ticker
     * does not exist).
     */
    private static boolean activityIsVisible = true;

    /**
     * These are the possible sorts of {@link #stocks} that can be created by
     * clicking the different sort (different list transformations) MenuItems in
     * this Activity's Menu. This enum is used to represent how stocks is
     * sorted, if at all.
     */
    public enum RvSort {
        NO_SORT,
        TICKER_ASC, TICKER_DESC,
        PRICE_ASC, PRICE_DESC,
        CHANGE_PERCENT_ASC, CHANGE_PERCENT_DESC

    }

    /**
     * Maps each {@link RvSort#toString()} -> {@link RvSort}. Used in {@link
     * #onCreate(Bundle)} to initialize {@link #rvSort}.
     */
    private static final Map<String, RvSort> stringToRvSortMap =
            new HashMap<String, RvSort>() {
                {
                    for (final RvSort s : RvSort.values()) {
                        put(s.toString(), s);
                    }
                }
            };

    /**
     * The list of ConcreteStockWithEhVals that are shown in this Activity.
     * {@link #rvAdapter} has a reference to this and performs operations on
     * this, including removing a Stock from this whenever a Stock is
     * swipe-deleted from {@link #rv}.
     *
     * @see #tickerToIndexMap
     * @see StockSwipeAndDragCallback#onSwiped(RecyclerView.ViewHolder, int)
     */
    private ConcreteStockWithEhValsList stocks;

    /**
     * Maps tickers to indexes in {@link #stocks}. This is used as a O(1) way
     * to determine whether or not a Stock with a certain ticker is in stocks.
     * This needs to be kept up to date with stocks, so changes to stocks should
     * be paired with changes to this. For this reason, {@link
     * StockSwipeAndDragCallback} has a reference to this, and calls
     * tickerToIndexMap.remove whenever a Stock is swipe-deleted from {@link
     * #rv}.
     *
     * @see #updateTickerToIndexMap()
     * @see #updateTickerToIndexMap(int)
     * @see StockSwipeAndDragCallback#onSwiped(RecyclerView.ViewHolder, int)
     */
    private final Map<String, Integer> tickerToIndexMap = new HashMap<>();

    private StockRecyclerAdapter rvAdapter;
    private SearchView searchView;
    private SharedPreferences prefs;
    private RequestQueue requestQueue;
    private Timer timer;

    private RvSort rvSort;
    private MenuItem sortByTicker_menuItem;
    private MenuItem sortByPrice_menuItem;
    private MenuItem sortByChangePercent_menuItem;

    /**
     * Called from {@link FindStockTask#onPostExecute(Integer)}.
     * <p>
     * If status equals {@link FindStockTask.Status#IO_EXCEPTION}, make a toast
     * saying there is no internet connection. If status equals {@link
     * FindStockTask.Status#STOCK_DOES_NOT_EXIST}, make a toast saying that the
     * stock was not found. If the stock was found, start an
     * IndividualStockActivity for the found Stock, passing the Stock's
     * information to IndividualStockActivity through Intent extras.
     *
     * @param status       The {@link FindStockTask.Status} of the task
     * @param searchTicker The ticker that was searched for
     * @param stock        The StockInHomeActivity that has been initialized if
     *                     the stock was found
     */
    @Override
    public void onFindStockTaskCompleted(final int status, final String searchTicker,
                                         final StockInHomeActivity stock) {
        if (!activityIsVisible) {
            return;
        }

        switch (status) {
            case FindStockTask.Status.STOCK_EXISTS:
                // Go to individual stock activity
                final Intent intent = new Intent(this, IndividualStockActivity.class);
                intent.putExtra("Ticker", stock.getTicker());
                intent.putExtra("Name", stock.getName());
                intent.putExtra("Data", stock.getDataAsArray());

                // Equivalent to checking if searchTicker is in stocks
                intent.putExtra("Is in favorites", tickerToIndexMap.containsKey(stock.getTicker()));
                startActivity(intent);
                break;
            case FindStockTask.Status.STOCK_DOES_NOT_EXIST:
                Toast.makeText(HomeActivity.this,
                        searchTicker + " couldn't be found", Toast.LENGTH_SHORT).show();
                break;
            case FindStockTask.Status.IO_EXCEPTION:
                Toast.makeText(HomeActivity.this,
                        "No internet connection", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Initializes various components of this Activity.
     * <p>
     * This method sets the "Stock Added" and "Stock Removed" booleans in {@link
     * #prefs} to false. One of these values is set to true in {@link
     * IndividualStockActivity#onPause()}, if a Stock is added or removed from
     * favorites and this Activity needs to be aware of it. In addition to
     * setting either the "Stock Added" or "Stock Removed" booleans,
     * IndividualStockActivity.onPause() updates Tickers, Names, and Data TSV
     * to reflect any changes. Because 1) added or removed Stocks are reflected in
     * Tickers, Names, and Data TSV; and 2) {@link #initStocksFromPreferences()}
     * uses the information from Tickers, Names, and Data TSV to initialize
     * {@link #stocks}; we know that in this method, any Stocks that were added
     * or removed from another Activity (added or removed from prefs) are
     * correctly added/removed from stocks. Therefore, the "Stock Added" and
     * "Stock Removed" booleans are set to false, because whatever addition or
     * removal they represented has been taken care of.
     *
     * @param savedInstanceState The savedInstanceState is not used
     * @see #initStocksFromPreferences()
     * @see #initRecyclerView()
     * @see #initRvSortFromPreferences()
     * @see IndividualStockActivity#onPause()
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        setTitle("Stock Watch");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        requestQueue = Volley.newRequestQueue(this);
        stocks = new ConcreteStockWithEhValsList();
//        fillPreferencesWithRandomStocks(0); // Starter kit

        /* onCreate is only called if the app was just opened. These preferences
         * should only be true if a stock was added from an activity outside of
         * this activity (i.e. IndividualStockActivity). */
        prefs.edit().putBoolean("Stock Added", false).apply();
        prefs.edit().putBoolean("Stock Removed", false).apply();

        initStocksFromPreferences();
        initRecyclerView();
        initRvSortFromPreferences();


        checkForUpdates(); // ACRA
    }

    /**
     * Initialize {@link #stocks} using the information stored in preferences.
     * <p>
     * This method should only be called in {@link #onCreate(Bundle)}.
     */
    private void initStocksFromPreferences() {
        final String[] tickerArr = prefs.getString("Tickers TSV", "").split("\t");
        final String[] nameArr = prefs.getString("Names TSV", "").split("\t");
        final String[] dataArr = prefs.getString("Data TSV", "").split("\t");

        /* If there are stocks in favorites, initialize recycler view to show
         * tickers with the previous saved data. */
        if (!tickerArr[0].isEmpty()) {
            stocks.ensureCapacity(tickerArr.length);
            ConcreteStockWithEhVals curStock;
            String curTicker, curName;
            Stock.State curState;
            double curPrice, curChangePoint, curChangePercent,
                    curEhPrice, curEhChangePoint, curEhChangePercent;
            for (int tickerNdx = 0, dataNdx = 0; tickerNdx < tickerArr.length; tickerNdx++, dataNdx += 7) {
                // nameNdx is the same as tickerNdx (1 ticker for 1 name, unlike Data TSV)
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
                curEhPrice = parseDouble(dataArr[dataNdx + 4]);
                curEhChangePoint = parseDouble(dataArr[dataNdx + 5]);
                curEhChangePercent = parseDouble(dataArr[dataNdx + 6]);

                curStock = new ConcreteStockWithEhVals(
                        curState, curTicker, curName,
                        curPrice, curChangePoint, curChangePercent,
                        curEhPrice, curEhChangePoint, curEhChangePercent);

                // Fill stocks and tickerToIndexMap
                stocks.add(curStock);
                tickerToIndexMap.put(curTicker, tickerNdx);
            }
        }
    }

    /**
     * Initialize {@link #rv}. This includes setting rv's {@link
     * StockRecyclerAdapter} to {@link #rvAdapter}, and attaching a {@link
     * StockSwipeAndDragCallback} to rv. The {@link
     * StockRecyclerAdapter.OnItemClickListener} for rv is also defined here to
     * start an IndividualStockActivity for the clicked Stock - the Stock's
     * information is passed to IndividualStockActivity through Intent extras.
     */
    private void initRecyclerView() {
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new StockRecyclerDivider(this));
        rvAdapter = new StockRecyclerAdapter(stocks, (ConcreteStockWithEhVals stock) -> {
            // Go to individual stock activity
            final Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", stock.getTicker());
            intent.putExtra("Name", stock.getName());
            intent.putExtra("Data", stock.getDataAsArray());

            // Equivalent to checking if ticker is in stocks
            intent.putExtra("Is in favorites", tickerToIndexMap.containsKey(stock.getTicker()));
            startActivity(intent);
        });
        rv.setAdapter(rvAdapter);
        // Init swipe to delete for rv.
        final StockSwipeAndDragCallback stockSwipeAndDragCallback =
                new StockSwipeAndDragCallback(this, rvAdapter, stocks, tickerToIndexMap);
        new ItemTouchHelper(stockSwipeAndDragCallback).attachToRecyclerView(rv);
    }

    /**
     * Initializes {@link #rvSort} using the {@link RvSort#toString()} value
     * stored in preferences. When this is called, the MenuItem titles do not
     * reflect an RvSort - for example, if rvSort equals {@link
     * RvSort#TICKER_ASC}, {@link #sortByTicker_menuItem} does not show an up
     * arrow. Because this Activity's Menu has not been created yet, and our
     * references to the MenuItems are not initialized yet, this method does not
     * call updateMenuItemTitles(). updateMenuItemTitles is first called in
     * {@link #onCreateOptionsMenu(Menu)}.
     *
     * @see #onCreateOptionsMenu(Menu)
     */
    private void initRvSortFromPreferences() {
        final String rvSortStr = prefs.getString("HomeActivity Stocks Sort", "");
        rvSort = stringToRvSortMap.getOrDefault(rvSortStr, RvSort.NO_SORT);
    }

    /**
     * This method sets {@link #activityIsVisible} to true. This also
     * re-initializes {@link #timer} because it is invalidated when {@link
     * #onPause()} is called, then uses timer to call {@link #updateStocks()} on
     * a constant interval. Additionally, this method checks if a Stock has been
     * added or removed outside of this activity, and if so, updates {@link
     * #stocks} and {@link #tickerToIndexMap} to reflect the change.
     *
     * @see #checkIfStockAddedFromOtherActivity()
     * @see #checkIfStockRemovedFromOtherActivity()
     */
    @Override
    protected void onResume() {
        super.onResume();
        activityIsVisible = true;

        checkIfStockAddedFromOtherActivity();
        checkIfStockRemovedFromOtherActivity();

        // If there are stocks in favorites, update stocks and rv
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!stocks.isEmpty()) {
                    updateStocks();
                }
            }
        };

        timer = new Timer();

        // Run every 10 seconds, starting immediately
        timer.schedule(timerTask, 0, 10000);


        checkForCrashes();
    }

    /**
     * Checks the values of the "Stock Added" boolean in {@link #prefs} to see
     * if there is a Stock stored in {@link #prefs} that is new to this Activity
     * (added from a different Activity). If there is a new Stock, this method
     * adds the new Stock into {@link #stocks} and updates {@link
     * #tickerToIndexMap}, while maintaining {@link #rvSort}.
     * <p>
     * The only way that a Stock is added to favorites is if the user stars the
     * Stock in {@link IndividualStockActivity}. If the user does this,
     * IndividualStockActivity sets the "Stock Added" boolean in prefs to true,
     * which allows other Activities to check preferences and be aware of the
     * new Stock. If the "Stock Added" boolean is true, then the following
     * strings in prefs contain the information needed to add the new Stock into
     * stocks and tickerToIndexMap:
     * <ul>
     * <li>"Stock Added Ticker"
     * <li>"Stock Added Name"
     * <li>"Stock Added Data" (7 element TSV)
     * </ul>
     * <p>
     * If there is a new Stock, this method calls {@link #sortStocksToRvSort()}
     * to ensure that the new Stock maintains the {@link RvSort} of stocks. If
     * rvSort equals {@link RvSort#NO_SORT}, this method inserts the new Stock
     * at index 0.
     *
     * @see IndividualStockActivity#addStockToPreferences()
     * @see IndividualStockActivity#onPause()
     */
    private void checkIfStockAddedFromOtherActivity() {
        final boolean stockAdded = prefs.getBoolean("Stock Added", false);
        if (!stockAdded) {
            return;
        }

        final String ticker = prefs.getString("Stock Added Ticker", "");
        final String name = prefs.getString("Stock Added Name", "");
        final String[] dataArr = prefs.getString("Stock Added Data", "").split("\t");
        final Stock.State state = Util.stringToStateMap.get(dataArr[0]);
        final double price = parseDouble(dataArr[1]);
        final double changePoint = parseDouble(dataArr[2]);
        final double changePercent = parseDouble(dataArr[3]);
        final double ehPrice, ehChangePoint, ehChangePercent;
        if (state == AFTER_HOURS || state == PREMARKET) {
            ehPrice = parseDouble(dataArr[4]);
            ehChangePoint = parseDouble(dataArr[5]);
            ehChangePercent = parseDouble(dataArr[6]);
        } else {
            // dataArr.length() equals 4, not 7
            ehPrice = 0;
            ehChangePoint = 0;
            ehChangePercent = 0;
        }

        final ConcreteStockWithEhVals added = new ConcreteStockWithEhVals(
                state, ticker, name,
                price, changePoint, changePercent,
                ehPrice, ehChangePoint, ehChangePercent);

        // Insert the added stock at the top of stocks
        stocks.add(0, added);
        // Maintain rvSort, then update tickerToIndexMap
        sortStocksToRvSort();
        updateTickerToIndexMap();

        // Restore prefs
        prefs.edit().putBoolean("Stock Added", false).apply();
    }

    /**
     * Checks the value of the "Stock Removed" boolean in {@link #prefs} to see
     * if there is a Stock that is no longer stored in prefs but is still
     * contained in {@link #stocks} and {@link #tickerToIndexMap}. This scenario
     * can occur if a Stock was removed from prefs from a different Activity
     * (IndividualStockActivity).
     * <p>
     * If the "Stock Removed" boolean is true, then the following strings in
     * prefs contain the information needed to remove the Stock from stocks and
     * tickerToIndexMap:
     * <ul>
     * <li>"Stock Removed Ticker"
     * </ul>
     * <p>
     *
     * @see IndividualStockActivity#addStockToPreferences()
     * @see IndividualStockActivity#onPause()
     */
    private void checkIfStockRemovedFromOtherActivity() {
        final boolean stockRemoved = prefs.getBoolean("Stock Removed", false);
        if (stockRemoved) {
            final String removedTicker =
                    prefs.getString("Stock Removed Ticker", "");
            final int removedIndex = tickerToIndexMap.get(removedTicker);

            tickerToIndexMap.remove(removedTicker); // Do before removal from stocks
            rvAdapter.remove(removedIndex); // Removes from rview and from stocks, and updates rview
            updateTickerToIndexMap(removedIndex);

            // Restore prefs
            prefs.edit().putBoolean("Stock Removed", false).apply();
            prefs.edit().putString("Stock Removed Ticker", "").apply();
        }
    }

    /**
     * This method sets {@link #activityIsVisible} to false, stops calls to
     * {@link #updateStocks()} by cancelling {@link #timer}, and saves {@link
     * #stocks} and {@link #rvSort} to preferences - stocks is saved to Tickers,
     * Names, and Data TSV in prefs.
     */
    @Override
    protected void onPause() {
        super.onPause();
        activityIsVisible = false;

        // cancel() invalidates timer - it must be re-initialized to use again
        timer.cancel();

        prefs.edit().putString("Tickers TSV", stocks.getStockTickersAsTSV()).apply();
        prefs.edit().putString("Names TSV", stocks.getStockNamesAsTSV()).apply();
        prefs.edit().putString("Data TSV", stocks.getStockDataAsTSV()).apply();

        prefs.edit().putString("HomeActivity Stocks Sort", rvSort.toString()).apply();

        unregisterManagers();
    }

    /**
     * Sends the user to the home Activity.
     */
    @Override
    public void onBackPressed() {
        final Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    /**
     * Cancels all the {@link MultiStockRequest} in {@link #requestQueue}.
     */
    @Override
    protected void onStop() {
        super.onStop();
        requestQueue.cancelAll(this);
    }

    /**
     * Partitions Stocks in {@link #stocks} into sets with a maximum size of 10.
     * The maximum size of a partition is 10 because the Market Watch
     * multiple-stock-website only supports displaying up to 10 stocks. The
     * tickers from each partition's Stocks are used to build a URL for the
     * Market Watch multiple-stock-website. For each partition, a {@link
     * MultiStockRequest} is created to update the Stocks in that partition.
     * Each partition's MultiStockRequest is added to {@link #requestQueue}.
     */
    private void updateStocks() {
        /* During this function's lifetime, the user could swipe-delete a stock.
         * Using the original stocks in stocks allows us to not worry about the
         * consequences of a stock being removed from stocks. As a result, this
         * function, as well as MultiStockRequest could possibly edit stocks
         * that have been removed from stocks. HomeActivity.onResponse() handles
         * this by ensuring that stocks contains a stock before updating the
         * UI. */
        final ConcreteStockWithEhValsList stocksToUpdate = new ConcreteStockWithEhValsList(stocks);
        final int numStocksTotal = stocksToUpdate.size();
        int numStocksUpdated = 0;

        // URL form: <base URL><ticker 1>,<ticker 2>,<ticker 3>,<ticker n>
        final String BASE_URL_MULTI = "https://www.marketwatch.com/investing/multi?tickers=";
        /* Up to 10 stocks are shown in the MarketWatch view multiple stocks
         * website. The first 10 tickers listed in the URL are shown. Appending
         * more than 10 tickers onto the URL has no effect on the website - the
         * first 10 tickers will be shown. */
        final StringBuilder tickersPartUrl = new StringBuilder(50); // Approximate size

        ConcreteStockWithEhValsList stocksToUpdateThisIteration;
        int numStocksToUpdateThisIteration;
        while (numStocksUpdated < numStocksTotal) {
            // Ex: if we've already updated 30 / 37 stocks, finish only 7 on the last iteration
            if (numStocksTotal - numStocksUpdated >= 10) {
                numStocksToUpdateThisIteration = 10;
            } else {
                numStocksToUpdateThisIteration = numStocksTotal - numStocksUpdated;
            }

            stocksToUpdateThisIteration = new ConcreteStockWithEhValsList(
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

    /**
     * Called when a response is received from a queued {@link
     * MultiStockRequest}. References to the Stocks in {@link #stocks} are
     * passed to each MultiStockRequest. Using these references,
     * MultiStockRequest can update the Stocks in stocks. This method notifies
     * {@link #rvAdapter} that updated Stocks have been changed, so that the
     * rvAdapter can update {@link #rv}.
     * <p>
     * If rv is updated while a cell is swiping or dragging, the action will be
     * stopped (as if the user lifted their finger off the screen). This method
     * avoids this by using {@link StockRecyclerAdapter#isSwipingOrDragging()}
     * to see if the user is currently swiping or dragging. If the user is
     * swiping or dragging, updating the current Stock is skipped. If the user
     * is not swiping or dragging, rvAdapter is notified that the current Stock
     * has changed. The index in stocks of the current Stock is determined
     * using {@link #tickerToIndexMap}.
     * <p>
     * This method also checks that tickerToIndexMap contains the ticker of the
     * current Stock to update before notifying rvAdapter. This is because the
     * user could have possible swipe-deleted the current Stock from stocks in
     * the time that the MultiStockRequest was executing.
     *
     * @param updatedStocks The ConcreteStockWithEhValsList with a maximum size
     *                      of 10 that contains the updated Stocks
     */
    @Override
    public void onResponse(final ConcreteStockWithEhValsList updatedStocks) {
        for (final Stock s : updatedStocks) {
            if (!rvAdapter.isSwipingOrDragging() && tickerToIndexMap.containsKey(s.getTicker())) {
                rvAdapter.notifyItemChanged(tickerToIndexMap.get(s.getTicker()));
            }
        }
    }

    /**
     * @param error The error resulting from a {@link MultiStockRequest}
     */
    @Override
    public void onErrorResponse(final VolleyError error) {
        if (error.getLocalizedMessage() != null && !error.getLocalizedMessage().isEmpty()) {
            Log.e("VolleyError", error.getLocalizedMessage());
        } else {
            Log.e("VolleyError",
                    "VolleyError thrown in HomeActivity.onErrorResponse, but error is null.");
        }
    }

    /**
     * Initializes the contents of this Activity's standard options menu. Also
     * calls {@link #updateMenuItemTitles()} to update the titles of the
     * just-initialized MenuItems to reflect the value of {@link #rvSort}.
     *
     * @param menu The Menu containing the list transformation MenuItems
     * @return True because we want the menu to be shown
     * @see #initRvSortFromPreferences()
     * @see #updateMenuItemTitles()
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_activity, menu);

        searchView = (SearchView) menu.findItem(R.id.menuItem_search).getActionView();
        searchView.setQueryHint("Ticker");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            /**
             * Called when the user submits the query. This could be due to a
             * key press on the keyboard or due to pressing a submit button.
             * <p>
             * This method checks if the query matches a loose regular
             * expression that would not match with an illegal ticker.
             * Potentially valid tickers are between [1,5] characters long, with
             * all characters being either digits, letters, or '.'. If query
             * does not match with the regular expression, a toast is shown to
             * notify the user. If query matches with the regular expression, a
             * {@link FindStockTask} is created using query as the ticker for
             * the FindStockTask. The result of the FindStockTask is handled in
             * {@link HomeActivity#onFindStockTaskCompleted(int, String,
             * StockInHomeActivity)}.
             *
             * @param query The query text that is to be submitted
             * @return True because the query has been handled by this
             */
            @Override
            public boolean onQueryTextSubmit(final String query) {
                final String ticker = query.trim().toUpperCase();

                final boolean isValidTicker = ticker.matches("[0-9A-Z.]{1,6}");

                if (isValidTicker) {
                    /* Prevent spamming of submit button. This collapses the
                     * keyboard. This works better than setSubmitButtonEnabled. */
                    searchView.clearFocus();

                    FindStockTask task = new FindStockTask(ticker, HomeActivity.this);
                    task.execute();
                } else {
                    Toast.makeText(HomeActivity.this,
                            ticker + " is an invalid symbol", Toast.LENGTH_SHORT).show();
                }

                return true;
            }

            /**
             * Called when the query text is changed by the user.
             *
             * @param newText The new content of the query text field
             * @return False because the SearchView should perform the default
             * action of showing any suggestions if available
             */
            @Override
            public boolean onQueryTextChange(final String newText) {
                return false;
            }
        });

        sortByTicker_menuItem = menu.findItem(R.id.menuItem_sortAlphabetically);
        sortByPrice_menuItem = menu.findItem(R.id.menuItem_sortByPrice);
        sortByChangePercent_menuItem = menu.findItem(R.id.menuItem_sortByChangePercent);

        /* rvSort is initialized in initRvSortFromPreferences(), but the
         * MenuItem titles do not reflect the current value of rvSort. the
         * MenuItem titles cannot be updated in initRvSortFromPreferences,
         * because this Activity does not have a reference to the MenuItems yet.
         * Now that we have references to the MenuItems, update the MenuItem
         * titles to reflect the value of rvSort. */
        updateMenuItemTitles();

        return true;
    }

    /**
     * This method handles the selection of a {@link MenuItem} from the options
     * menu of this Activity. All of the MenuItems in the options menu are
     * list transformations on {@link #rv}. All of these list transformations
     * change the indexing of the Stocks in {@link #stocks}. Therefore, each of
     * these list transformations must call {@link #updateTickerToIndexMap()}.
     * Additionally, {@link #sortStocksToRvSort()} is called to update the
     * MenuItem titles to reflect a change in value of {@link #rvSort}.
     *
     * @param item The selected MenuItem
     * @return True if a known item was selected, otherwise call the super
     * method
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        /* The ordering of if statements within each case block determines
         * whether the ascending or descending sort is selected initially, if
         * neither were selected before. */
        switch (item.getItemId()) {
            case R.id.menuItem_sortAlphabetically:
                if (rvSort == RvSort.TICKER_ASC) {
                    rvSort = RvSort.TICKER_DESC;
                } else {
                    rvSort = RvSort.TICKER_ASC;
                }
                break;
            case R.id.menuItem_sortByPrice:
                if (rvSort == RvSort.PRICE_DESC) {
                    rvSort = RvSort.PRICE_ASC;
                } else {
                    rvSort = RvSort.PRICE_DESC;
                }
                break;
            case R.id.menuItem_sortByChangePercent:
                if (rvSort == RvSort.CHANGE_PERCENT_DESC) {
                    rvSort = RvSort.CHANGE_PERCENT_ASC;
                } else {
                    rvSort = RvSort.CHANGE_PERCENT_DESC;
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        sortStocksToRvSort();
        updateTickerToIndexMap();
        rvAdapter.notifyItemRangeChanged(0, rvAdapter.getItemCount());

        updateMenuItemTitles();

        return true;
    }

    /**
     * Updates the following MenuItems to reflect the value of {@link #rvSort}.
     * <ul>
     * <li>{@link #sortByTicker_menuItem}
     * <li>{@link #sortByPrice_menuItem}
     * <li>{@link #sortByChangePercent_menuItem}
     * </ul>
     * <p>
     * An example of this is: if rvSort equals {@link RvSort#TICKER_ASC},
     * sortByTicker_menuItem should show an upwards arrow in its title, and
     * sortByPrice_menuItem and sortByChangePercent_menuItem should not show any
     * arrows.
     */
    private void updateMenuItemTitles() {
        switch (rvSort) {
            case TICKER_ASC:
                sortByTicker_menuItem.setTitle(getString(R.string.ticker_asc_menuItemTitle));

                sortByPrice_menuItem.setTitle(getText(R.string.price_menuItemTitle));
                sortByChangePercent_menuItem.setTitle(getText(R.string.changePercent_menuItemTitle));
                break;
            case TICKER_DESC:
                sortByTicker_menuItem.setTitle(getString(R.string.ticker_desc_menuItemTitle));

                sortByPrice_menuItem.setTitle(getText(R.string.price_menuItemTitle));
                sortByChangePercent_menuItem.setTitle(getText(R.string.changePercent_menuItemTitle));
                break;
            case PRICE_ASC:
                sortByPrice_menuItem.setTitle(getText(R.string.price_asc_menuItemTitle));

                sortByTicker_menuItem.setTitle(getString(R.string.ticker_menuItemTitle));
                sortByChangePercent_menuItem.setTitle(getText(R.string.changePercent_menuItemTitle));
                break;
            case PRICE_DESC:
                sortByPrice_menuItem.setTitle(getText(R.string.price_desc_menuItemTitle));

                sortByTicker_menuItem.setTitle(getString(R.string.ticker_menuItemTitle));
                sortByChangePercent_menuItem.setTitle(getText(R.string.changePercent_menuItemTitle));
                break;
            case CHANGE_PERCENT_ASC:
                sortByChangePercent_menuItem.setTitle(getText(R.string.changePercent_asc_menuItemTitle));

                sortByTicker_menuItem.setTitle(getString(R.string.ticker_menuItemTitle));
                sortByPrice_menuItem.setTitle(getText(R.string.price_menuItemTitle));
                break;
            case CHANGE_PERCENT_DESC:
                sortByChangePercent_menuItem.setTitle(getText(R.string.changePercent_desc_menuItemTitle));

                sortByTicker_menuItem.setTitle(getString(R.string.ticker_menuItemTitle));
                sortByPrice_menuItem.setTitle(getText(R.string.price_menuItemTitle));
                break;
            case NO_SORT:
            default:
                sortByTicker_menuItem.setTitle(getString(R.string.ticker_menuItemTitle));
                sortByPrice_menuItem.setTitle(getText(R.string.price_menuItemTitle));
                sortByChangePercent_menuItem.setTitle(getText(R.string.changePercent_menuItemTitle));
                break;
        }
    }

    /**
     * This function should be called when a Stock is removed by being
     * swipe-deleted. This function updates the indexes in {@link #stocks} that
     * a ticker in {@link #tickerToIndexMap} maps to. After a swipe-deletion,
     * the Stocks in range [index of removed Stock, size of stocks] of stocks
     * need to be updated. The Stocks in this range are now at their previous
     * index - 1 in stocks. This function does not remove any mappings from
     * tickerToIndexMap.
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
     * Updates the indexes in {@link #stocks} that a ticker in {@link
     * #tickerToIndexMap} maps to. This method clears tickerToIndexMap and
     * reconstructs it with the current indexing of stocks. This should be
     * called anytime after the indexing of stocks changes. This is equivalent
     * to calling {@link #updateTickerToIndexMap(int)} and passing in 0.
     */
    public synchronized void updateTickerToIndexMap() {
        tickerToIndexMap.clear();
        for (int i = 0; i < stocks.size(); i++) {
            tickerToIndexMap.put(stocks.get(i).getTicker(), i);
        }
    }

    /**
     * Sorts {@link #stocks} to the sort specified by {@link #rvSort}.
     */
    private void sortStocksToRvSort() {
        boolean needToSort = true;
        final Comparator<StockInHomeActivity> comparator;

        switch (rvSort) {
            case TICKER_ASC:
                comparator = Comparator.comparing(StockInHomeActivity::getTicker);
                break;
            case TICKER_DESC:
                comparator = Comparator.comparing(StockInHomeActivity::getTicker).reversed();
                break;
            case PRICE_ASC:
                comparator = Comparator.comparingDouble(StockInHomeActivity::getLivePrice);
                break;
            case PRICE_DESC:
                comparator = Comparator.comparingDouble(StockInHomeActivity::getLivePrice).reversed();
                break;
            case CHANGE_PERCENT_ASC:
                comparator = Comparator.comparingDouble(StockInHomeActivity::getNetChangePercent);
                break;
            case CHANGE_PERCENT_DESC:
                comparator = Comparator.comparingDouble(StockInHomeActivity::getNetChangePercent).reversed();
                break;
            case NO_SORT:
            default:
                needToSort = false;
                comparator = null;
        }

        if (needToSort) {
            stocks.sort(comparator);
        }
    }

    /**
     * This method sets {@link #rvSort} to {@link RvSort#NO_SORT}, and then
     * calls {@link #updateMenuItemTitles()}.
     * <p>
     * This method is called in {@link
     * StockSwipeAndDragCallback#onChildDrawOver(Canvas, RecyclerView,
     * RecyclerView.ViewHolder, float, float, int, boolean)} to notify this
     * activity that rvSort is no longer valid in the case that Stocks
     * in {@link #stocks} are swapped (switch indexes). This is because a single
     * swap invalidates any sort that stocks had.
     *
     * @see StockSwipeAndDragCallback#onChildDrawOver(Canvas, RecyclerView,
     * RecyclerView.ViewHolder, float, float, int, boolean)
     */
    public void notifyRvSortInvalidated() {
        rvSort = RvSort.NO_SORT;
        updateMenuItemTitles();
    }

    /**
     * Fills Tickers TSV with tickers from the NASDAQ, sets all of Data TSV's
     * numeric values to -1, and sets Data TSV's {@link Stock.State} values to
     * {@link Stock.State#CLOSED}. Fills Names TSV with "[ticker] name", because
     * we don't know the names of the Stocks, only their tickers. The largest
     * number of Stocks that can be added is the number of companies in the
     * NASDAQ.
     *
     * @param size The number of Stocks to put in preferences
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

    /**
     * Override to call {@link #unregisterManagers()}, which is required for
     * use of HockeyApp.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterManagers();
    }

    /**
     * Required for use of HockeyApp.
     */
    private void checkForCrashes() {
        CrashManager.register(this);
    }

    /**
     * Required for use of HockeyApp.
     */
    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this);
    }

    /**
     * Required for use of HockeyApp.
     */
    private void unregisterManagers() {
        UpdateManager.unregister();
    }


    /**
     * An AsyncTask that checks if a stock exists by checking if it exists on
     * the WSJ website.
     */
    private static class FindStockTask extends AsyncTask<Void, Integer, Integer> {

        private final String searchTicker;
        private StockInHomeActivity stock;
        private final WeakReference<FindStockTaskListener> completionListener;

        private FindStockTask(final String searchTicker, final FindStockTaskListener completionListener) {
            this.searchTicker = searchTicker;
            this.completionListener = new WeakReference<>(completionListener);
        }

        /**
         * Connects to the WSJ website for the stock with ticker equal to {@link
         * #searchTicker}. This function then checks for a specific Element that
         * only exists on the WSJ stock-not-found page.
         * <p>
         * If an {@link IOException} is thrown while connecting to the WSJ
         * website, this returns {@link Status#IO_EXCEPTION}. If the stock does
         * not exist, this returns {@link Status#STOCK_DOES_NOT_EXIST}. If the
         * stock exists, the information needed to create a {@link Stock} are
         * parsed and used to initialize {@link #stock} as a {@link
         * StockInHomeActivity}, and {@link Status#STOCK_EXISTS} is returned.
         *
         * @param voids Take no parameters
         * @return The Status of the task
         */
        @Override
        protected Integer doInBackground(final Void... voids) {
            int status = -1;

            final String URL = "https://quotes.wsj.com/" + searchTicker;

            Document doc;
            try {
                doc = Jsoup.connect(URL)
                        .timeout(20000)
                        .get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                doc = null;
                status = Status.IO_EXCEPTION;
            }

            if (doc != null) {
                final Element contentFrame = doc.selectFirst(
                        "html > body > div.pageFrame > div.contentFrame");

                // If the stock's page is found on WSJ, this element does not exist
                final Element flagElmnt = contentFrame.selectFirst(
                        "div[class$=notfound_header module]");

                status = (flagElmnt == null) ? Status.STOCK_EXISTS : Status.STOCK_DOES_NOT_EXIST;

                if (status == Status.STOCK_EXISTS) {
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
                        final double ehPrice, ehChangePoint, ehChangePercent;
                        // Remove ',' or '%' that could be in strings
                        ehPrice = parseDouble(subData.selectFirst(
                                "span#ms_quote_val").ownText().replaceAll("[^0-9.]+", ""));
                        final Elements ah_diffs = subData.select(
                                "span[id] > span");
                        ehChangePoint = parseDouble(ah_diffs.get(0).ownText().replaceAll("[^0-9.-]+", ""));
                        ehChangePercent = parseDouble(ah_diffs.get(1).ownText().replaceAll("[^0-9.-]+", ""));

                        stateStr = subData.selectFirst("span").ownText();
                        state = stateStr.equals("AFTER HOURS") ? AFTER_HOURS : PREMARKET;

                        stock = new ConcreteStockWithEhVals(state, searchTicker, name,
                                price, changePoint, changePercent,
                                ehPrice, ehChangePoint, ehChangePercent);
                    } else {
                        stateStr = mainData.selectFirst("span.timestamp_label").ownText();
                        state = stateStr.equals("REAL TIME") ? OPEN : CLOSED;

                        stock = new ConcreteStock(state, searchTicker, name,
                                price, changePoint, changePercent);
                    }
                }
            }

            return status;
        }

        /**
         * Notifies {@link #completionListener} that the task is complete.
         * <p>
         * {@link #searchTicker} is passed to completionListener so that
         * completionListener will know what ticker was searched for, in the
         * case that Status is not equal to {@link Status#STOCK_EXISTS} and
         * {@link #stock} is not initialized.
         *
         * @param status The Status of the task
         */
        @Override
        protected void onPostExecute(final Integer status) {
            completionListener.get().onFindStockTaskCompleted(status, searchTicker, stock);
        }


        interface Status {

            int STOCK_EXISTS = 1;
            int STOCK_DOES_NOT_EXIST = 2;
            int IO_EXCEPTION = 3;

        }

    }

}
