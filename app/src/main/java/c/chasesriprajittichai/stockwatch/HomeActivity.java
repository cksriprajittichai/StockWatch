package c.chasesriprajittichai.stockwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import c.chasesriprajittichai.stockwatch.AsyncTaskListeners.FindStockTaskListener;
import c.chasesriprajittichai.stockwatch.Stocks.BasicStock;

import static c.chasesriprajittichai.stockwatch.Stocks.BasicStock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.Stocks.BasicStock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.Stocks.BasicStock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.Stocks.BasicStock.State.PREMARKET;
import static java.lang.Double.parseDouble;

public class HomeActivity extends AppCompatActivity implements FindStockTaskListener,
        Response.Listener<String>, Response.ErrorListener {

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
                Element flagElement = doc.selectFirst("html > body[role=document][class~=page--quote symbol--(Stock|AmericanDepositoryReceiptStock) page--Index]");

                stockExists = (flagElement != null);
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

    private final ArrayList<BasicStock> mstocks = new ArrayList<>();
    private RecyclerView mrecyclerView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Stock Watch");

        mrequestQueue = Volley.newRequestQueue(this);

        mpreferences = PreferenceManager.getDefaultSharedPreferences(this);

        /* Starter kit */
//        fillPreferencesWithRandomStocks(0);

        String tickersCSV = mpreferences.getString("Tickers CSV", "");
        String[] tickers = tickersCSV.split(","); // "".split(",") returns {""}
        String dataCSV = mpreferences.getString("Data CSV", "");
        String[] data = dataCSV.split(","); // "".split(",") returns {""}

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
                switch (data[dataNdx].toLowerCase(Locale.US)) {
                    case "premarket":
                        curState = PREMARKET;
                        break;
                    case "open":
                        curState = OPEN;
                        break;
                    case "after_hours":
                        curState = AFTER_HOURS;
                        break;
                    case "closed":
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

                /* Fill mstocks, mtickerToStockMap, and mtickerToIndexMap. */
                mstocks.add(curStock);
                mtickerToStockMap.put(curTicker, curStock);
                mtickerToIndexMap.put(curTicker, tickerNdx);
            }
        }

        mrecyclerView = findViewById(R.id.recyclerView_home);
        mrecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mrecyclerView.addItemDecoration(new RecyclerHomeDivider(this));
        mrecyclerView.setAdapter(new RecyclerHomeAdapter(mstocks, basicStock -> {
            // Go to individual stock activity
            Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", basicStock.getTicker());
            // Equivalent to checking if ticker is in mtickerToIndexMap or in mstocks
            intent.putExtra("Is in favorites", mtickerToStockMap.containsKey(basicStock.getTicker()));
            startActivity(intent);
        }));
        setUpItemTouchHelper();
    }

    private void setUpItemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleItemTouchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            // Minimize the amount of object allocation done in onChildDraw()
            final Drawable background = new ColorDrawable(Color.RED);
            final Drawable garbageIcon = ContextCompat.getDrawable(HomeActivity.this, R.drawable.ic_delete_black_24dp);
            final float dpUnit = HomeActivity.this.getResources().getDisplayMetrics().density;
            final int garbageMargin = (int) (16 * dpUnit);

            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(RecyclerView rv, RecyclerView.ViewHolder viewHolder) {
                return super.getSwipeDirs(rv, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                RecyclerHomeAdapter adapter = (RecyclerHomeAdapter) mrecyclerView.getAdapter();
                int position = viewHolder.getAdapterPosition();
                final String removeTicker = mstocks.get(position).getTicker();

                mtickerToStockMap.remove(removeTicker);
                mtickerToIndexMap.remove(removeTicker);
                updateTickerToIndexMap(position);
                mstocks.remove(position);

                adapter.notifyItemRemoved(position);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView rv, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                /* This method can be called on ViewHolders that have already been swiped away.
                 * Ignore these. */
                if (viewHolder.getAdapterPosition() == -1) {
                    return;
                }

                // Draw red background
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                // Draw garbage can
                int itemHeight = itemView.getBottom() - itemView.getTop();
                int intrinsicWidth = garbageIcon.getIntrinsicWidth();
                int intrinsicHeight = garbageIcon.getIntrinsicHeight();

                int xMarkLeft = itemView.getRight() - garbageMargin - intrinsicWidth;
                int xMarkRight = itemView.getRight() - garbageMargin;
                int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int xMarkBottom = xMarkTop + intrinsicHeight;
                garbageIcon.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

                garbageIcon.draw(c);

                super.onChildDraw(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchHelper);
        itemTouchHelper.attachToRecyclerView(mrecyclerView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If there are stocks in favorites, update mstocks and mrecyclerView.
        if (!mstocks.isEmpty()) {
            updateStocks();
        }
    }

    /**
     * Partitions stocks in mstocks into sets of maximum size 10. The maximum size of each partition
     * is 10 because the Market Watch multiple-stock-website only supports displaying up to 10
     * stocks. The tickers from each partition's stocks are used to build a URL for the Market
     * Watch multiple-stock-website. The URL is then sent into a Volley request queue. The parsing
     * of the HTML retrieved from the websites and updating of variables happens in onResponse().
     */
    private void updateStocks() {
        final ArrayList<String> tickers = getStockTickers();
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
        Elements valueRoots, tickers, states, prices, priceChangeRoots, priceChanges, priceChangePercents;

        Document doc = Jsoup.parse(response);

        valueRoots = doc.select("body > div[id=blanket] div[id=maincontent] > div[class^=block multiquote] div[class~=section activeQuote bgQuote (down|up)?]");

        tickers = valueRoots.select("div[class=ticker] > a[href][title]");
        states = valueRoots.select("div[class=marketheader] > p[class=column marketstate]");
        prices = valueRoots.select("div[class=lastprice] > div[class=pricewrap] > p[class=data bgLast]");
        priceChangeRoots = valueRoots.select("div[class=lastpricedetails] > p[class=lastcolumn data]");
        priceChanges = priceChangeRoots.select("span[class=bgChange]");
        priceChangePercents = priceChangeRoots.select("span[class=bgPercentChange]");

        final int numStocksToUpdate = tickers.size();

        // Iterate through mstocks that we're updating
        for (int i = 0; i < numStocksToUpdate; i++) {
            switch (states.get(i).text().toLowerCase(Locale.US)) {
                case "premarket": // Individual stock site uses this
                case "before the bell": // Multiple stock view site uses this
                    curState = PREMARKET;
                    break;
                case "open":
                    curState = OPEN;
                    break;
                case "after hours":
                    curState = AFTER_HOURS;
                    break;
                case "market closed": // Multiple stock view site uses this
                case "closed":
                    curState = CLOSED;
                    break;
                default:
                    curState = OPEN; /** Create error case. */
                    break;
            }
            curTicker = tickers.get(i).text();
            // Remove ',' or '%' that could be in strings
            curPrice = parseDouble(prices.get(i).text().replaceAll("[^0-9.]+", ""));
            curChangePoint = parseDouble(priceChanges.get(i).text().replaceAll("[^0-9.-]+", ""));
            curChangePercent = parseDouble(priceChangePercents.get(i).text().replaceAll("[^0-9.-]+", ""));

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
    protected void onPause() {
        super.onPause();

        mpreferences.edit().putString("Tickers CSV", getStockTickersAsCSV()).apply();
        mpreferences.edit().putString("Data CSV", getStockDataAsCSV()).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_activity, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.searchMenuItem);
        msearchView = (SearchView) searchMenuItem.getActionView();

        msearchView.setEnabled(true);
        msearchView.setQueryHint("Ticker");
        msearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                final String ticker = query.trim().toUpperCase();

                // Parse query before creating a FindStockTask.
                /* Valid tickers are between [1,5] characters long, will all characters being either
                 * letters, numbers, '.', or '-'. */
                boolean isValidTicker = true;
                if (ticker.length() <= 0 || ticker.length() > 5) {
                    isValidTicker = false;
                }
                for (char c : ticker.toCharArray()) {
                    if (!Character.isLetterOrDigit(c) && c != '.') {
                        isValidTicker = false;
                        break;
                    }
                }

                if (isValidTicker) {
                    /* The thread executing the FindStockTask will call onFindStockTaskCompleted()
                     * when completed. */
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
        /* None of these list transformations needs to update preferences. Any changes that happen
         * to mstocks while between calls onResume() and onPause() should not update preferences.
         * When onPause() is called, preferences are updated to reflect the stocks in mstocks. */
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
                Comparator<BasicStock> ascendingPriceComparator = Comparator.comparingDouble(BasicStock::getPrice);
                Comparator<BasicStock> descendingPriceComparator = ascendingPriceComparator.reversed();
                mstocks.sort(descendingPriceComparator);
                updateTickerToIndexMap();

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
                return true;
            case R.id.sortByPercentChangeMenuItem:
                // Sort by decreasing magnitude of daily price change percent
                mstocks.sort((BasicStock a, BasicStock b) -> {
                    // Ignore sign, want stocks with the largest percent change (magnitude only)
                    double aMagnitude = Math.abs(a.getChangePercent());
                    double bMagnitude = Math.abs(b.getChangePercent());

                    return Double.compare(aMagnitude, bMagnitude) * -1; // Flip to get descending
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
     * Removes a stock from mpreferences; removes a stock from Tickers CSV and Data CSV.
     * If the stock is not found (mticker is not found in Tickers CSV), this function does nothing.
     *
     * @param ticker Ticker of the stock to remove.
     * @return True if stock was removed, false otherwise.
     */
    private boolean removeStockFromPreferences(String ticker) {
        final String tickersCSV = mpreferences.getString("Tickers CSV", "");
        final String[] tickerArr = tickersCSV.split(","); // "".split(",") returns {""}

        if (!tickerArr[0].isEmpty()) {
            final ArrayList<String> tickerList = new ArrayList<>(Arrays.asList(tickerArr));

            int tickerNdx = tickerList.indexOf(ticker);
            if (tickerNdx != -1) {
                /* Delete stock's ticker. */
                tickerList.remove(tickerNdx);
                mpreferences.edit().putString("Tickers CSV", String.join(",", tickerList)).apply();

                /* Delete stock's data. */
                String dataCSV = mpreferences.getString("Data CSV", "");
                final ArrayList<String> dataList = new ArrayList<>(Arrays.asList(dataCSV.split(",")));

                // 4 data elements per 1 ticker. DataNdx is the index of the first element to delete.
                int dataNdx = tickerNdx * 4;
                for (int deleteCount = 1; deleteCount <= 4; deleteCount++) { // Delete 4 data elements
                    dataList.remove(dataNdx);
                }
                mpreferences.edit().putString("Data CSV", String.join(",", dataList)).apply();
                return true;
            }
        }

        return false;
    }

    /**
     * @return A CSV string of the tickers of the stocks in mstocks.
     */
    private String getStockTickersAsCSV() {
        return String.join(",", getStockTickers());
    }

    /**
     * Stock data includes the stock's state, price, change point, and change percent.
     *
     * @return A CSV string of the data of the stocks in mstocks.
     */
    private String getStockDataAsCSV() {
        final int size = mstocks.size();
        final ArrayList<BasicStock.State> states = getStockStates();
        final ArrayList<Double> prices = getStockPrices();
        final ArrayList<Double> changePoints = getStockChangePoints();
        final ArrayList<Double> changePercents = getStockChangePercents();

        final ArrayList<String> data = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            data.add(states.get(i).toString() + ',' + prices.get(i) + ',' +
                    changePoints.get(i) + ',' + changePercents.get(i));
        }

        return String.join(",", data);
    }

    private ArrayList<BasicStock.State> getStockStates() {
        final ArrayList<BasicStock.State> states = new ArrayList<>(mstocks.size());
        for (BasicStock s : mstocks) {
            states.add(s.getState());
        }
        return states;
    }

    private ArrayList<String> getStockTickers() {
        final ArrayList<String> tickers = new ArrayList<>(mstocks.size());
        for (BasicStock s : mstocks) {
            tickers.add(s.getTicker());
        }
        return tickers;
    }

    private ArrayList<Double> getStockPrices() {
        final ArrayList<Double> prices = new ArrayList<>(mstocks.size());
        for (BasicStock s : mstocks) {
            prices.add(s.getPrice());
        }
        return prices;
    }

    private ArrayList<Double> getStockChangePoints() {
        final ArrayList<Double> changePoints = new ArrayList<>(mstocks.size());
        for (BasicStock s : mstocks) {
            changePoints.add(s.getChangePoint());
        }
        return changePoints;
    }

    private ArrayList<Double> getStockChangePercents() {
        final ArrayList<Double> changePercents = new ArrayList<>(mstocks.size());
        for (BasicStock s : mstocks) {
            changePercents.add(s.getChangePercent());
        }
        return changePercents;
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
