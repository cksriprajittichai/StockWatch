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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import c.chasesriprajittichai.stockwatch.listeners.FindStockTaskListener;
import c.chasesriprajittichai.stockwatch.listeners.StockSwipeLeftListener;
import c.chasesriprajittichai.stockwatch.stocks.BasicStock;
import c.chasesriprajittichai.stockwatch.stocks.StockList;

import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.PREMARKET;
import static java.lang.Double.parseDouble;

public class HomeActivity extends AppCompatActivity implements FindStockTaskListener,
        StockSwipeLeftListener, Response.Listener<String>, Response.ErrorListener {

    private static class FindStockTask extends AsyncTask<Void, Integer, Boolean> {

        private String mticker;
        private WeakReference<FindStockTaskListener> mcompletionListener;

        private FindStockTask(String ticker, FindStockTaskListener completionListener) {
            this.mticker = ticker;
            this.mcompletionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final String baseUrl = "https://www.marketwatch.com/tools/quotes/lookup.asp?lookup=";
            String url = baseUrl + mticker;

            boolean stockExists = false;
            try {
                Document doc = Jsoup.connect(url).get();

                // This element exists if the stock's individual page is found on MarketWatch
                Element foundElement = doc.selectFirst("html > body[role=document][class~=page--quote symbol--(Stock|AmericanDepositoryReceiptStock) page--Index]");

                stockExists = (foundElement != null);
            } catch (IOException ioe) {
                /* Show "No internet connection", or something. */
                Log.e("IOException", ioe.getLocalizedMessage());
            }

            return stockExists;
        }

        @Override
        protected void onPostExecute(Boolean stockExists) {
            mcompletionListener.get().onFindStockTaskCompleted(mticker, stockExists);
        }
    }

    @BindView(R.id.recyclerView_home) RecyclerView mrecyclerView;

    private final StockList mstocks = new StockList();
    private SearchView msearchView;
    private SharedPreferences mpreferences;
    private RequestQueue mrequestQueue;

    /* Maps tickers to BasicStock objects in mstocks. */
    private final HashMap<String, BasicStock> mtickerToStockMap = new HashMap<>();

    /* Maps tickers to indexes in mstocks. */
    private final HashMap<String, Integer> mtickerToIndexMap = new HashMap<>();

    /* Called from FindStockTask.onPostExecute(). */
    @Override
    public void onFindStockTaskCompleted(String ticker, boolean stockExists) {
        if (stockExists) {
            // Go to individual stock activity
            Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", ticker);
            // Equivalent to checking if ticker is in mtickerToIndexMap or in mstocks
            intent.putExtra("Is in favorites", mtickerToStockMap.containsKey(ticker));
            startActivity(intent);
        } else {
            msearchView.setQuery("", false);
            Toast.makeText(HomeActivity.this, ticker + " couldn't be found", Toast.LENGTH_SHORT).show();
        }
    }

    /* Called from StockSwipeLeftCallback.onSwipe(). */
    @Override
    public void onStockSwipedLeft(int position) {
        RecyclerAdapter adapter = (RecyclerAdapter) mrecyclerView.getAdapter();
        final String removeTicker = mstocks.get(position).getTicker();

        mtickerToStockMap.remove(removeTicker);
        mtickerToIndexMap.remove(removeTicker);
        updateTickerToIndexMap(position);
        mstocks.remove(position);

        adapter.notifyItemRemoved(position);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Stock Watch");
        ButterKnife.bind(this);

        mrequestQueue = Volley.newRequestQueue(this);
        mpreferences = PreferenceManager.getDefaultSharedPreferences(this);

        /* Starter kit */
//        fillPreferencesWithRandomStocks(500);

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
                    case "CLOSEDO":
                        curState = CLOSED;
                        break;
                    default:
                        curState = OPEN; /** Create error case. */
                        break;
                }
                curPrice = parseDouble(data[dataNdx + 1]);
                curChangePoint = parseDouble(data[dataNdx + 2]);
                curChangePercent = parseDouble(data[dataNdx + 3]);

                curStock = new BasicStock(curState, curTicker, curPrice, curChangePoint, curChangePercent);

                // Fill mstocks, mtickerToStockMap, and mtickerToIndexMap
                mstocks.add(curStock);
                mtickerToStockMap.put(curTicker, curStock);
                mtickerToIndexMap.put(curTicker, tickerNdx);
            }
        }

        mrecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mrecyclerView.addItemDecoration(new RecyclerDivider(this));
        mrecyclerView.setAdapter(new RecyclerAdapter(mstocks, basicStock -> {
            // Go to individual stock activity
            Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", basicStock.getTicker());
            // Equivalent to checking if ticker is in mtickerToIndexMap or in mstocks
            intent.putExtra("Is in favorites", mtickerToStockMap.containsKey(basicStock.getTicker()));
            startActivity(intent);
        }));
        // Init swipe to delete for mrecyclerView.
        StockSwipeLeftCallback stockSwipeLeftCallback = new StockSwipeLeftCallback(this, this);
        new ItemTouchHelper(stockSwipeLeftCallback).attachToRecyclerView(mrecyclerView);

        Log.d("test", "onCreate() called");
    }

    @Override
    protected void onResume() {
        super.onResume();


        // If there are stocks in favorites, update mstocks and mrecyclerView.
        if (!mstocks.isEmpty()) {
            updateStocks();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mpreferences.edit().putString("Tickers CSV", mstocks.getStockTickersAsCSV()).apply();
        mpreferences.edit().putString("Data CSV", mstocks.getStockDataAsCSV()).apply();
    }

    /**
     * Partitions stocks in mstocks into sets of maximum size 10. The maximum size of each partition
     * is 10 because the Market Watch multiple-stock-website only supports displaying up to 10
     * stocks. The tickers from each partition's stocks are used to build a URL for the Market
     * Watch multiple-stock-website. The URL is then sent into a Volley request queue. The parsing
     * of the HTML retrieved from the websites and updating of variables happens in onResponse().
     */
    private void updateStocks() {
        final ArrayList<String> tickers = mstocks.getStockTickers();
        final int numStocksTotal = tickers.size();
        int numStocksUpdated = 0;

        // URL form: <base URL><mticker 1>,<mticker 2>,<mticker 3>,<mticker n>
        final String baseUrl = "https://www.marketwatch.com/investing/multi?tickers=";

        /* Up to 10 stocks are shown in the MarketWatch view multiple stocks website. The first
         * 10 tickers listed in the URL are shown. Appending more than 10 tickers onto the URL
         * has no effect on the website - the first 10 tickers will be shown. */
        StringBuilder url = new StringBuilder(50); // Approximate size

        StringRequest stringRequest;
        int numStocksToUpdateThisIteration, i;
        while (numStocksUpdated < numStocksTotal) {
            // Ex: if we've already updated 30 / 37 stocks, finish only 7 on the last iteration
            if (numStocksTotal - numStocksUpdated >= 10) {
                numStocksToUpdateThisIteration = 10;
            } else {
                numStocksToUpdateThisIteration = numStocksTotal - numStocksUpdated;
            }

            url.append(baseUrl);
            // Append tickers for stocks that will be updated in this iteration
            for (i = numStocksUpdated; i < numStocksUpdated + numStocksToUpdateThisIteration; i++) {
                url.append(tickers.get(i));
                url.append(',');
            }
            url.deleteCharAt(url.length() - 1); // Delete extra comma

            // URL is now finished. Get HTML from URL and parse and fill mstocks.
            stringRequest = new StringRequest(Request.Method.GET, url.toString(), this, this);
            mrequestQueue.add(stringRequest);

            numStocksUpdated += numStocksToUpdateThisIteration;
            url.setLength(0); // Clear URL
        }
    }

    @Override
    public void onResponse(String response) {
        BasicStock curStock;
        String curTicker;
        BasicStock.State curState;
        double curPrice, curChangePoint, curChangePercent;
        Elements quoteRoots, live_valueRoots, tickers, states, live_prices, live_changeRoots,
                live_changePoints, live_changePercents, close_valueRoots, close_prices,
                close_changeRoots, close_changePoints, close_changePercents;

        /* Prices and changes gathered now are the current values. For example, if a stock's is in
         * the AFTER_HOURS state, then it's price, change point, and change percent will be the
         * stock's current price, after hours change point, and after hours change percent. */

        Document doc = Jsoup.parse(response);
        quoteRoots = doc.select("body > div[id=blanket] div[id=maincontent] > div[class^=block multiquote] > div[class^=quotedisplay]");

        live_valueRoots = quoteRoots.select("div[class^=section activeQuote bgQuote]");
        tickers = live_valueRoots.select("div[class=ticker] > a[href][title]");
        states = live_valueRoots.select("div[class=marketheader] > p[class=column marketstate]");
        live_prices = live_valueRoots.select("div[class=lastprice] > div[class=pricewrap] > p[class=data bgLast]");
        live_changeRoots = live_valueRoots.select("div[class=lastpricedetails] > p[class=lastcolumn data]");
        live_changePoints = live_changeRoots.select("span[class=bgChange]");
        live_changePercents = live_changeRoots.select("span[class=bgPercentChange]");

        close_valueRoots = quoteRoots.select("div[class=prevclose section bgQuote] > div[class=offhours]");
        close_prices = close_valueRoots.select("p[class=lastcolumn data bgLast price]");
        close_changeRoots = close_valueRoots.select("p[class=lastcolumn data]");
        close_changePoints = close_changeRoots.select("span[class=bgChange]");
        close_changePercents = close_changeRoots.select("span[class=bgPercentChange]");

        final int numStocksToUpdate = tickers.size();

        /* curPrice will be displayed on mrecyclerView. If a stock's state is OPEN or CLOSED, then
         * its display price should be the live price. If a stock's state is PREMARKET OR
         * AFTER_HOURS, then its display price should be the last price that the stock closed at.
         * The same logic applies for curChangePoint and curChangePercent. */
        boolean curDataShouldBeCloseData;

        // Iterate through mstocks that we're updating
        for (int i = 0; i < numStocksToUpdate; i++) {
            switch (states.get(i).text().toLowerCase(Locale.US)) {
                case "premarket": // Individual stock site uses this
                case "before the bell": // Multiple stock view site uses this
                    curState = PREMARKET;
                    curDataShouldBeCloseData = true;
                    break;
                case "open":
                    curState = OPEN;
                    curDataShouldBeCloseData = false;
                    break;
                case "after hours":
                    curState = AFTER_HOURS;
                    curDataShouldBeCloseData = true;
                    break;
                case "market closed": // Multiple stock view site uses this
                case "closed":
                    curState = CLOSED;
                    curDataShouldBeCloseData = false;
                    break;
                default:
                    curState = OPEN; /** Create error case. */
                    curDataShouldBeCloseData = false;
                    break;
            }

            curTicker = tickers.get(i).text();
            // Remove ',' or '%' that could be in strings
            if (curDataShouldBeCloseData) {
                curPrice = parseDouble(close_prices.get(i).text().replaceAll("[^0-9.]+", ""));
                curChangePoint = parseDouble(close_changePoints.get(i).text().replaceAll("[^0-9.-]+", ""));
                curChangePercent = parseDouble(close_changePercents.get(i).text().replaceAll("[^0-9.-]+", ""));
            } else {
                curPrice = parseDouble(live_prices.get(i).text().replaceAll("[^0-9.]+", ""));
                curChangePoint = parseDouble(live_changePoints.get(i).text().replaceAll("[^0-9.-]+", ""));
                curChangePercent = parseDouble(live_changePercents.get(i).text().replaceAll("[^0-9.-]+", ""));
            }

            /* mstocks points to the same BasicStock objects that mtickerToStockMap has pointers to.
             * So by updating curStock, which points to a BasicStock in mtickerToStockMap, we are
             * also updating mstocks. */
            curStock = mtickerToStockMap.get(curTicker);
            curStock.setState(curState);
            curStock.setPrice(curPrice);
            curStock.setChangePoint(curChangePoint);
            curStock.setChangePercent(curChangePercent);

            mrecyclerView.getAdapter().notifyItemChanged(mtickerToIndexMap.get(curTicker));
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e("VolleyError", error.getLocalizedMessage());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_activity, menu);

        msearchView = (SearchView) menu.findItem(R.id.searchMenuItem).getActionView();
        msearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                final String ticker = query.trim().toUpperCase();

                /* Valid tickers are between [1,5] characters long, with all characters being
                 * either digits, letters, or '.'. */
                boolean isValidTicker = ticker.matches("[0-9A-Z.]{1,6}");

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
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* All of these list transformations change the indexing of the stocks in mstocks.
         * Therefore, each of these list transformations must update tickerToIndexMap. */
        switch (item.getItemId()) {
            case R.id.sortAlphabeticallyMenuItem:
                Comparator<BasicStock> tickerComparator = Comparator.comparing(BasicStock::getTicker);
                mstocks.sort(tickerComparator);
                updateTickerToIndexMap();

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
                return true;
            case R.id.sortByPriceMenuItem:
                // Sort by decreasing price
                Comparator<BasicStock> descendingPriceComparator = Comparator.comparingDouble(BasicStock::getPrice).reversed();
                mstocks.sort(descendingPriceComparator);
                updateTickerToIndexMap();

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
                return true;
            case R.id.sortByPercentChangeMenuItem:
                // Sort by decreasing magnitude of change percent
                mstocks.sort((BasicStock a, BasicStock b) -> {
                    // Ignore sign, compare change percents by magnitude
                    return Double.compare(Math.abs(b.getChangePercent()), Math.abs(a.getChangePercent()));
                });
                updateTickerToIndexMap();

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
                return true;
            case R.id.shuffleMenuItem:
                Collections.shuffle(mstocks);
                updateTickerToIndexMap();

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
                return true;
            case R.id.flipListMenuItem:
                Collections.reverse(mstocks);
                updateTickerToIndexMap();

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This function should be called when a stock is removed by being swiped away. This function
     * updates the indexes in mstocks that a ticker in mtickerToIndexMap maps to. After a swipe
     * deletion, the stocks in range [index of stock removed, size of mstocks] of mstocks need to
     * be updated. The stocks in this range are now at their previous index - 1. This function does
     * not remove any mappings from mtickerToIndexMap.
     *
     * @param lastRemovedIndex The index of stock most recently removed.
     */
    private void updateTickerToIndexMap(int lastRemovedIndex) {
        if (lastRemovedIndex >= mstocks.size()) {
            // If the last removed stock was the last stock in mstocks
            return;
        }

        for (int i = lastRemovedIndex; i < mstocks.size(); i++) {
            mtickerToIndexMap.put(mstocks.get(i).getTicker(), i);
        }
    }

    /**
     * Updates the indexes in mstocks that a ticker in mtickerToIndexMap maps to. This function
     * clears mtickerToIndexMap and rebuilds it with the current stock indexing of mstocks. This
     * function is equivalent to calling updateTickerToIndexMap(0).
     */
    private void updateTickerToIndexMap() {
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
        String tickersStr = getString(R.string.nasdaqTickers);
        String[] tickerArr = tickersStr.split(",");
        boolean usingMaxSize = false;
        if (size >= tickerArr.length) {
            size = tickerArr.length;
            usingMaxSize = true;
        }

        if (usingMaxSize) {
            mpreferences.edit().putString("Tickers CSV", String.join(",", tickerArr)).apply();
        } else {
            String[] subTickerArr = new String[size];
            System.arraycopy(tickerArr, 0, subTickerArr, 0, size);
            mpreferences.edit().putString("Tickers CSV", String.join(",", subTickerArr)).apply();
        }

        String[] dataArr = new String[4 * size];
        for (int i = 0; i < size * 4; i += 4) {
            dataArr[i] = BasicStock.State.CLOSED.toString();
            dataArr[i + 1] = "-1";
            dataArr[i + 2] = "-1";
            dataArr[i + 3] = "-1";
        }
        mpreferences.edit().putString("Data CSV", String.join(",", dataArr)).apply();
    }

}
