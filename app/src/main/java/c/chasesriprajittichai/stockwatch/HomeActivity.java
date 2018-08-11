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
import c.chasesriprajittichai.stockwatch.stocks.BasicStock;
import c.chasesriprajittichai.stockwatch.stocks.BasicStockList;

import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.PREMARKET;
import static java.lang.Double.parseDouble;


public final class HomeActivity extends AppCompatActivity implements FindStockTaskListener,
        Response.Listener<BasicStockList>, Response.ErrorListener {

    private static class FindStockTask extends AsyncTask<Void, Integer, Boolean> {

        private final String mticker;
        private final WeakReference<FindStockTaskListener> mcompletionListener;

        private FindStockTask(final String ticker, final FindStockTaskListener completionListener) {
            this.mticker = ticker;
            this.mcompletionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected Boolean doInBackground(final Void... params) {
            final String URL = "https://quotes.wsj.com/" + mticker;

            boolean stockExists = false;
            try {
                final Document doc = Jsoup.connect(URL).get();

                // If the stock's page is found on WSJ, this element does not exist
                final Element flagElmnt = doc.selectFirst(
                        "html > body > div.pageFrame > div.contentFrame div[class$=notfound_header module]");

                stockExists = (flagElmnt == null);
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
            }

            return stockExists;
        }

        @Override
        protected void onPostExecute(final Boolean stockExists) {
            mcompletionListener.get().onFindStockTaskCompleted(mticker, stockExists);
        }
    }


    @BindView(R.id.recyclerView_home) RecyclerView mrecyclerView;

    private BasicStockList mstocks;
    private RecyclerAdapter mrecyclerAdapter;
    private SearchView msearchView;
    private SharedPreferences mpreferences;
    private RequestQueue mrequestQueue;
    private Timer mtimer;

    // Maps tickers to indexes in mstocks
    private final Map<String, Integer> mtickerToIndexMap = new HashMap<>();

    /* Called from FindStockTask.onPostExecute(). */
    @Override
    public void onFindStockTaskCompleted(final String ticker, final boolean stockExists) {
        if (stockExists) {
            // Go to individual stock activity
            final Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", ticker);
            // Equivalent to checking if ticker is in mstocks
            intent.putExtra("Is in favorites", mtickerToIndexMap.containsKey(ticker));
            startActivity(intent);
        } else {
            msearchView.setQuery("", false);
            Toast.makeText(HomeActivity.this, ticker + " couldn't be found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Stock Watch");
        ButterKnife.bind(this);
        mpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mrequestQueue = Volley.newRequestQueue(this);
        mstocks = new BasicStockList();

        /* Starter kit */
//        fillPreferencesWithRandomStocks(0);

        final String[] tickers = mpreferences.getString("Tickers CSV", "").split(","); // "".split(",") returns {""}
        final String[] data = mpreferences.getString("Data CSV", "").split(","); // "".split(",") returns {""}
        /* If there are stocks in favorites, initialize recycler view to show tickers with the
         * previous data. */
        if (!tickers[0].isEmpty()) {
            mstocks.ensureCapacity(tickers.length);
            BasicStock curStock;
            String curTicker;
            BasicStock.State curState;
            double curPrice, curChangePoint, curChangePercent;
            for (int tickerNdx = 0, dataNdx = 0; tickerNdx < tickers.length; tickerNdx++, dataNdx += 4) {
                curTicker = tickers[tickerNdx];
                switch (data[dataNdx]) {
                    case "PREMARKET":
                        curState = PREMARKET;
                        break;
                    case "OPEN":
                        curState = OPEN;
                        break;
                    case "AFTER_HOURS":
                        curState = AFTER_HOURS;
                        break;
                    case "CLOSED":
                        curState = CLOSED;
                        break;
                    case "ERROR":
                    default:
                        Log.e("ErrorStateInHomeActivity", String.format(
                                "Stock with state equal to BasicStock.State.ERROR in HomeActivity.%n" +
                                        "Ticker: %s", curTicker));
                        // MultiStockRequest should never return stocks with the ERROR state
                        // Do not add this error stock to mstocks or mtickerToIndexMap
                        continue;
                }
                curPrice = parseDouble(data[dataNdx + 1]);
                curChangePoint = parseDouble(data[dataNdx + 2]);
                curChangePercent = parseDouble(data[dataNdx + 3]);

                curStock = new BasicStock(curState, curTicker, curPrice, curChangePoint, curChangePercent);

                // Fill mstocks and mtickerToIndexMap
                mstocks.add(curStock);
                mtickerToIndexMap.put(curTicker, tickerNdx);
            }
        }

        mrecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mrecyclerView.addItemDecoration(new RecyclerDivider(this));
        mrecyclerAdapter = new RecyclerAdapter(mstocks, basicStock -> {
            // Go to individual stock activity
            final Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", basicStock.getTicker());
            // Equivalent to checking if ticker is in mstocks
            intent.putExtra("Is in favorites", mtickerToIndexMap.containsKey(basicStock.getTicker()));
            startActivity(intent);
        });
        mrecyclerView.setAdapter(mrecyclerAdapter);
        // Init swipe to delete for mrecyclerView.
        final StockSwipeAndDragCallback stockSwipeAndDragCallback =
                new StockSwipeAndDragCallback(this, mrecyclerAdapter, mstocks, mtickerToIndexMap);
        new ItemTouchHelper(stockSwipeAndDragCallback).attachToRecyclerView(mrecyclerView);

        checkForUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If there are stocks in favorites, update mstocks and mrecyclerView
        mtimer = new Timer();
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!mstocks.isEmpty()) {
                    updateStocks();
                }
            }
        };
        // Run every 10 seconds, starting immediately
        mtimer.schedule(timerTask, 0, 10000);

        checkForCrashes();
    }

    @Override
    protected void onPause() {
        super.onPause();

        /* Stop updating. This ruins mtimer and mtimerTask, so they must be re-initilized to be
         * used again. */
        mtimer.cancel();

        mpreferences.edit().putString("Tickers CSV", mstocks.getStockTickersAsCSV()).apply();
        mpreferences.edit().putString("Data CSV", mstocks.getStockDataAsCSV()).apply();

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
        mrequestQueue.cancelAll(this);
    }

    /**
     * Partitions stocks in mstocks into sets of maximum size 10. The maximum size of each partition
     * is 10 because the Market Watch multiple-stock-website only supports displaying up to 10
     * stocks. The tickers from each partition's stocks are used to build a URL for the Market
     * Watch multiple-stock-website. For each partition, a MultiStockRequest is created using the
     * URL and the references to the stocks represented in the URL. The MultiStockRequest is then
     * added to mrequestQueue.
     */
    private void updateStocks() {
        /* During this function's lifetime, the user could swipe-delete a stock. Using the original
         * stocks in mstocks allows us to not worry about the consequences of a stock being removed
         * from mstocks. As a result, this function, as well as MultiStockRequest could possibly
         * edit stocks that have been removed from mstocks. HomeActivity.onResponse() handles this
         * by ensuring that mstocks contains a stock before updating the UI. */
        final BasicStockList stocksToUpdate = new BasicStockList(mstocks);
        final int numStocksTotal = stocksToUpdate.size();
        int numStocksUpdated = 0;

        // URL form: <base URL><mticker 1>,<mticker 2>,<mticker 3>,<mticker n>
        final String BASE_URL_MULTI = "https://www.marketwatch.com/investing/multi?tickers=";
        /* Up to 10 stocks are shown in the MarketWatch view multiple stocks website. The first
         * 10 tickers listed in the URL are shown. Appending more than 10 tickers onto the URL
         * has no effect on the website - the first 10 tickers will be shown. */
        final StringBuilder tickersPartUrl = new StringBuilder(50); // Approximate size

        BasicStockList stocksToUpdateThisIteration;
        int numStocksToUpdateThisIteration;
        while (numStocksUpdated < numStocksTotal) {
            // Ex: if we've already updated 30 / 37 stocks, finish only 7 on the last iteration
            if (numStocksTotal - numStocksUpdated >= 10) {
                numStocksToUpdateThisIteration = 10;
            } else {
                numStocksToUpdateThisIteration = numStocksTotal - numStocksUpdated;
            }

            stocksToUpdateThisIteration = new BasicStockList(stocksToUpdate.subList(numStocksUpdated,
                    numStocksUpdated + numStocksToUpdateThisIteration));

            // Append tickers for stocks that will be updated in this iteration
            for (BasicStock s : stocksToUpdateThisIteration) {
                tickersPartUrl.append(s.getTicker());
                tickersPartUrl.append(',');
            }
            tickersPartUrl.deleteCharAt(tickersPartUrl.length() - 1); // Delete extra comma

            // Send stocks that will be updated and their URL to the MultiStockRequest.
            mrequestQueue.add(new MultiStockRequest(BASE_URL_MULTI + tickersPartUrl.toString(),
                    stocksToUpdateThisIteration, this, this));

            numStocksUpdated += numStocksToUpdateThisIteration;
            tickersPartUrl.setLength(0); // Clear tickers part of the URL
        }
    }

    @Override
    public void onResponse(final BasicStockList updatedStocks) {
        for (final BasicStock s : updatedStocks) {
            /* Updating the recycler view while dragging disrupts the dragging, causing the
             * currently dragged item to fall over whatever position is hovers. */
            /* The user could have swipe-deleted curStock in the time that the MultiStockRequest was
             * executing. If so, mtickerToIndexMap does not contain curTicker. */
            if (!mrecyclerAdapter.isDragging() && mtickerToIndexMap.containsKey(s.getTicker())) {
                mrecyclerAdapter.notifyItemChanged(mtickerToIndexMap.get(s.getTicker()));
            }
        }
    }

    @Override
    public void onErrorResponse(final VolleyError error) {
        if (error != null) {
            Log.e("VolleyError", error.getLocalizedMessage());
        } else {
            Log.e("NullVolleyError", "VolleyError thrown, but error is null.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_activity, menu);

        msearchView = (SearchView) menu.findItem(R.id.menuItem_search_home).getActionView();
        msearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                final String ticker = query.trim().toUpperCase();

                /* Valid tickers are between [1,5] characters long, with all characters being
                 * either digits, letters, or '.'. */
                final boolean isValidTicker = ticker.matches("[0-9A-Z.]{1,6}");

                if (isValidTicker) {
                    FindStockTask task = new FindStockTask(ticker, HomeActivity.this);
                    task.execute();

                    /* Prevent spamming of submit button. This also prevents multiple FindStockTasks
                     * from being executed at the same time. Similar effect as disabling the
                     * SearchView submit button. */
                    msearchView.clearFocus();
                } else {
                    Toast.makeText(HomeActivity.this, ticker + " is an invalid symbol", Toast.LENGTH_SHORT).show();
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
        /* All of these list transformations change the indexing of the stocks in mstocks.
         * Therefore, each of these list transformations must call updateTickerToIndexMap(). */
        switch (item.getItemId()) {
            case R.id.menuItem_sortAlphabetically_home:
                final Comparator<BasicStock> tickerComparator = Comparator.comparing(BasicStock::getTicker);
                mstocks.sort(tickerComparator);
                updateTickerToIndexMap();
                mrecyclerAdapter.notifyItemRangeChanged(0, mrecyclerAdapter.getItemCount());
                return true;
            case R.id.menuItem_sortByPrice_home:
                // Sort by decreasing price
                final Comparator<BasicStock> descendingPriceComparator = Comparator.comparingDouble(BasicStock::getPrice).reversed();
                mstocks.sort(descendingPriceComparator);
                updateTickerToIndexMap();
                mrecyclerAdapter.notifyItemRangeChanged(0, mrecyclerAdapter.getItemCount());
                return true;
            case R.id.menuItem_sortByChangePercent_home:
                // Sort by decreasing magnitude of change percent
                mstocks.sort((BasicStock a, BasicStock b) -> {
                    // Ignore sign, compare change percents by magnitude
                    return Double.compare(Math.abs(b.getChangePercent()), Math.abs(a.getChangePercent()));
                });
                updateTickerToIndexMap();
                mrecyclerAdapter.notifyItemRangeChanged(0, mrecyclerAdapter.getItemCount());
                return true;
            case R.id.menuItem_sortByState_home:
                final Comparator<BasicStock> stateComparator = Comparator.comparing(BasicStock::getState);
                mstocks.sort(stateComparator);
                updateTickerToIndexMap();
                mrecyclerAdapter.notifyItemRangeChanged(0, mrecyclerAdapter.getItemCount());
                return true;
            case R.id.menuItem_shuffle_home:
                Collections.shuffle(mstocks);
                updateTickerToIndexMap();
                mrecyclerAdapter.notifyItemRangeChanged(0, mrecyclerAdapter.getItemCount());
                return true;
            case R.id.menuItem_flipList_home:
                Collections.reverse(mstocks);
                updateTickerToIndexMap();
                mrecyclerAdapter.notifyItemRangeChanged(0, mrecyclerAdapter.getItemCount());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * UPDATE COMMENT?
     * This function should be called when a stock is removed by being swiped away. This function
     * updates the indexes in mstocks that a ticker in mtickerToIndexMap maps to. After a swipe
     * deletion, the stocks in range [index of stock removed, size of mstocks] of mstocks need to
     * be updated. The stocks in this range are now at their previous index - 1. This function does
     * not remove any mappings from mtickerToIndexMap.
     *
     * @param lastRemovedIndex The index of stock most recently removed.
     */
    public synchronized void updateTickerToIndexMap(final int lastRemovedIndex) {
        if (lastRemovedIndex >= mstocks.size()) {
            // If the last removed stock was the last stock in mstocks
            return;
        }

        for (int i = lastRemovedIndex; i < mstocks.size(); i++) {
            mtickerToIndexMap.put(mstocks.get(i).getTicker(), i);
        }
    }

    /**
     * UPDATE COMMENT?
     * Updates the indexes in mstocks that a ticker in mtickerToIndexMap maps to. This function
     * clears mtickerToIndexMap and rebuilds it with the current stock indexing of mstocks. This
     * should be called anytime after the indexing of stocks in mstocks changes. This is equivalent
     * to calling updateTickerToIndexMap(0).
     */
    public synchronized void updateTickerToIndexMap() {
        mtickerToIndexMap.clear();
        for (int i = 0; i < mstocks.size(); i++) {
            mtickerToIndexMap.put(mstocks.get(i).getTicker(), i);
        }
    }

    /**
     * Testing function. Fills Tickers CSV preference with all the tickers from the NASDAQ and fills
     * Data CSV preference with -1. The largest number of stocks that can be added is the number of
     * companies in the NASDAQ.
     *
     * @param size The number of stocks to put in preferences.
     */
    private void fillPreferencesWithRandomStocks(int size) {
        final String tickersStr = getString(R.string.nasdaqTickers);
        final String[] tickerArr = tickersStr.split(",");
        boolean usingMaxSize = false;
        if (size >= tickerArr.length) {
            size = tickerArr.length;
            usingMaxSize = true;
        }

        if (usingMaxSize) {
            mpreferences.edit().putString("Tickers CSV", TextUtils.join(",", tickerArr)).apply();
        } else {
            final String[] subTickerArr = new String[size];
            System.arraycopy(tickerArr, 0, subTickerArr, 0, size);
            mpreferences.edit().putString("Tickers CSV", TextUtils.join(",", subTickerArr)).apply();
        }

        final String[] dataArr = new String[4 * size];
        for (int i = 0; i < size * 4; i += 4) {
            dataArr[i] = BasicStock.State.CLOSED.toString();
            dataArr[i + 1] = "-1";
            dataArr[i + 2] = "-1";
            dataArr[i + 3] = "-1";
        }
        mpreferences.edit().putString("Data CSV", TextUtils.join(",", dataArr)).apply();
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
