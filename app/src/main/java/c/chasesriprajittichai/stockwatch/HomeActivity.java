package c.chasesriprajittichai.stockwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import c.chasesriprajittichai.stockwatch.AsyncTaskListeners.DownloadBasicStocksTaskListener;
import c.chasesriprajittichai.stockwatch.AsyncTaskListeners.FindStockTaskListener;

import static c.chasesriprajittichai.stockwatch.BasicStock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.BasicStock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.BasicStock.State.OPEN;
import static java.lang.Double.parseDouble;

public class HomeActivity extends AppCompatActivity implements FindStockTaskListener, DownloadBasicStocksTaskListener {

    private static class FindStockTask extends AsyncTask<Void, Integer, Boolean> {

        private String ticker;
        private WeakReference<FindStockTaskListener> completionListener;

        private FindStockTask(String ticker, FindStockTaskListener completionListener) {
            this.ticker = ticker;
            this.completionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final String baseUrl = "https://www.marketwatch.com/tools/quotes/lookup.asp?lookup=";
            String url = baseUrl + ticker;

            boolean stockExists = false;
            try {
                Document doc = Jsoup.connect(url).get();

                // This element exists if the stock's individual page is found on MarketWatch
                Element flagElement = doc.selectFirst("html > body[role=document][class=page--quote symbol--Stock page--Index]");

                stockExists = (flagElement != null);
            } catch (IOException ioe) {
                /* Show "No internet connection", or something. */
                Log.e("IOException", ioe.getLocalizedMessage());
            }

            return stockExists;
        }

        @Override
        protected void onPostExecute(Boolean stockExists) {
            completionListener.get().onFindStockTaskCompleted(ticker, stockExists);
        }
    }

    private static class DownloadBasicStocksTask extends AsyncTask<String, Integer, Integer> {

        private final int STATUS_GOOD = 0;
        private final int STATUS_IOEXCEPTION = 1;
        private WeakReference<ArrayList<BasicStock>> stocks;
        private WeakReference<DownloadBasicStocksTaskListener> completionListener;

        private DownloadBasicStocksTask(ArrayList<BasicStock> basicStocks,
                                        DownloadBasicStocksTaskListener completionListener) {
            this.stocks = new WeakReference<>(basicStocks);
            this.completionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected Integer doInBackground(String... tickers) {
            final int numStocksTotal = tickers.length;
            int numStocksUpdated = 0;
            int EXIT_STATUS = STATUS_GOOD;

            // URL form: <base URL><ticker 1>,<ticker 2>,<ticker 3>,<ticker n>
            final String baseUrl = "https://www.marketwatch.com/investing/multi?tickers=";

            /* Up to 10 stocks are shown in the MarketWatch view multiple stocks website. The first
             * 10 tickers listed in the URL are shown. Appending more than 10 tickers onto the URL
             * has no effect on the website - the first 10 tickers will be shown. */
            StringBuilder url = new StringBuilder(50); // Approximate size

            int numStocksToUpdateThisIteration, i, websiteNdx;
            double curPrice, curChange, curPercent;
            BasicStock.State curState;
            Elements quoteRoots, states, prices, priceChangeRoots, priceChanges, priceChangePercents;
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
                    url.append(tickers[i]);
                    url.append(',');
                }
                url.deleteCharAt(url.length() - 1); // Delete extra comma

                // URL is now finished. Get HTML from URL and parse and fill stocks.
                try {
                    Document doc = Jsoup.connect(url.toString()).get();

                    quoteRoots = doc.select("body > div[id=blanket] div[id=maincontent] div[class~=section activeQuote bgQuote (down|up)?]");

                    states = quoteRoots.select("div[class=marketheader] > p[class=column marketstate]");
                    prices = quoteRoots.select("div[class=lastprice] > div[class=pricewrap] > p[class=data bgLast]");
                    priceChangeRoots = quoteRoots.select("div[class=lastpricedetails] > p[class=lastcolumn data]");
                    priceChanges = priceChangeRoots.select("span[class=bgChange]");
                    priceChangePercents = priceChangeRoots.select("span[class=bgPercentChange]");

                    // Iterate through stocks that we're updating this iteration
                    for (i = numStocksUpdated, websiteNdx = 0; i < numStocksUpdated + numStocksToUpdateThisIteration; i++, websiteNdx++) {
                        switch (states.get(websiteNdx).text().toLowerCase(Locale.US)) {
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
                        // Remove ',' or '%' that could be in strings
                        curPrice = parseDouble(prices.get(websiteNdx).text().replaceAll("[^0-9.]+", ""));
                        curChange = parseDouble(priceChanges.get(websiteNdx).text().replaceAll("[^0-9.-]+", ""));
                        curPercent = parseDouble(priceChangePercents.get(websiteNdx).text().replaceAll("[^0-9.-]+", ""));

                        stocks.get().set(i, new BasicStock(curState, tickers[i], curPrice, curChange, curPercent));
                    }
                } catch (IOException ioe) {
                    EXIT_STATUS = STATUS_IOEXCEPTION;
                    Log.e("IOException", ioe.getLocalizedMessage());
                }

                numStocksUpdated += numStocksToUpdateThisIteration;
                url.setLength(0); // Clear URL
            }

            return EXIT_STATUS;
        }

        @Override
        protected void onPostExecute(Integer status) {
            if (status == STATUS_GOOD) {
                completionListener.get().onDownloadBasicStocksTaskCompleted();
            } else if (status == STATUS_IOEXCEPTION) {
                /* Show "No internet connection", or something. */
            }
        }
    }

    private ArrayList<BasicStock> stocks = new ArrayList<>();
    private RecyclerView recyclerView;
    private SearchView searchView;
    private SharedPreferences preferences;

    /* Called from DownloadBasicStocksTask.onPostExecute(). */
    @Override
    public void onDownloadBasicStocksTaskCompleted() {
        recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
    }

    /* Called from FindStockTask.onPostExecute(). */
    @Override
    public void onFindStockTaskCompleted(String ticker, boolean stockExists) {
        if (stockExists) {
            boolean isInFavorites = false;

            // Determine whether ticker isInFavorites already
            String[] tickers = preferences.getString("Tickers CSV", "").split(",");
            for (String t : tickers) {
                if (t.equalsIgnoreCase(ticker)) {
                    isInFavorites = true;
                    break;
                }
            }

            // Go to IndividualStockActivity
            Intent intent = new Intent(HomeActivity.this, IndividualStockActivity.class);
            intent.putExtra("Ticker", ticker);
            intent.putExtra("Is in favorites", isInFavorites);
            HomeActivity.this.startActivity(intent);
        } else {
            searchView.setQuery("", false);
            Toast.makeText(HomeActivity.this, ticker + " couldn't be found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Stock Watch");

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        /* Starter kit */
//        preferences.edit().putString("Tickers CSV", "").apply();
//        preferences.edit().putString("Data CSV", "").apply();
//        preferences.edit().putString("Tickers CSV", "GOOGL").apply();
//        preferences.edit().putString("Data CSV", "CLOSED,1002.89,3.41,3.04").apply();
//        preferences.edit().putString("Tickers CSV", "GOOGL,UTX,RTN,AAL,AAPL,ADBE").apply();
//        preferences.edit().putString("Tickers CSV", "BRK.A,BRK.B,AAL,AAPL,ADBE,AMD,AMZN,ANTM,AXP,BA,BAC,CAT,CI,CSCO,CTXS,CVX,DAL,DIS,DKS,DRYS,DWDP,FB,FSLR,GE,GM,GOOGL,GS,HD,IBM,INTC,JNJ,JPM,MRK,MSFT,NVDA,RTN,T,TRV,UTX").apply();

        String tickersCSV = preferences.getString("Tickers CSV", "");
        String[] tickers = tickersCSV.split(","); // "".split(",") returns {""}
        String dataCSV = preferences.getString("Data CSV", "");
        String[] data = dataCSV.split(","); // "".split(",") returns {""}

        /* If there are stocks in favorites initialize recycler view to show tickers with the
         * previous data. Otherwise, there must be no stocks in tickers. */
        if (!tickers[0].isEmpty()) {
            stocks.ensureCapacity(tickers.length);
            String curTicker;
            BasicStock.State curState;
            double curPrice, curChangePoint, curChangePercent;
            for (int tickerNdx = 0, dataNdx = 0; tickerNdx < tickers.length; tickerNdx++, dataNdx += 4) {
                curTicker = tickers[tickerNdx];
                switch (data[dataNdx].toLowerCase(Locale.US)) {
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

                stocks.add(new BasicStock(curState, curTicker, curPrice, curChangePoint, curChangePercent));
            }
        }

        recyclerView = findViewById(R.id.recyclerView_home);
        recyclerView.setAdapter(new RecyclerHomeAdapter(stocks, basicStock -> {
            Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", basicStock.getTicker());
            intent.putExtra("Is in favorites", true);
            startActivityForResult(intent, 1);
        }));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerHomeDivider(this));
    }

    @Override
    protected void onResume() {
        super.onResume();

        String tickersCSV = preferences.getString("Tickers CSV", "");
        String[] tickers = tickersCSV.split(","); // "".split(",") returns {""}
        // If there are stocks in favorites update stocks and recyclerView.
        if (!tickers[0].equals("")) {
            DownloadBasicStocksTask task = new DownloadBasicStocksTask(stocks, this);
            task.execute(tickers);
        } else if (!stocks.isEmpty()) {
            /* Tickers CSV preference is an empty string, meaning that there should be no stocks in
             * favorites. Stocks can only be removed one at a time, so there must have only been
             * one stock remaining in favorites. Remove it. */
            stocks.clear(); // Same effect as stocks.remove(0)
            recyclerView.getAdapter().notifyItemRemoved(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        preferences.edit().putString("Tickers CSV", getStockTickersAsCSV()).apply();
        preferences.edit().putString("Data CSV", getStockStateAndPricesAndChangesAsCSV()).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_activity, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.searchMenuItem);
        searchView = (SearchView) searchMenuItem.getActionView(); // Init searchView member

//        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setQueryHint("Ticker");
        searchView.setSubmitButtonEnabled(true); // Init as enabled
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
                    searchView.clearFocus();
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
        switch (item.getItemId()) {
            case R.id.sortAlphabeticallyMenuItem:
                Comparator<BasicStock> tickerComparator = Comparator.comparing(BasicStock::getTicker);
                stocks.sort(tickerComparator);

                preferences.edit().putString("Tickers CSV", getStockTickersAsCSV()).apply();

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;
            case R.id.sortByPriceMenuItem:
                // Sort by decreasing price
                Comparator<BasicStock> ascendingPriceComparator = Comparator.comparingDouble(BasicStock::getPrice);
                Comparator<BasicStock> descendingPriceComparator = ascendingPriceComparator.reversed();
                stocks.sort(descendingPriceComparator);

                preferences.edit().putString("Tickers CSV", getStockTickersAsCSV()).apply();

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;
            case R.id.sortByPercentChangeMenuItem:
                // Sort by decreasing magnitude of daily price change percent
                stocks.sort((BasicStock a, BasicStock b) -> {
                    // Ignore sign, want stocks with the largest percent change (magnitude only)
                    double aMagnitude = Math.abs(a.getChangePercent());
                    double bMagnitude = Math.abs(b.getChangePercent());

                    return Double.compare(aMagnitude, bMagnitude) * -1; // Flip to get descending
                });

                preferences.edit().putString("Tickers CSV", getStockTickersAsCSV()).apply();

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;
            case R.id.shuffleMenuItem:
                Collections.shuffle(stocks);

                preferences.getString("Tickers CSV", "");
                preferences.edit().putString("Tickers CSV", getStockTickersAsCSV()).apply();

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private ArrayList<BasicStock.State> getStockStates() {
        final ArrayList<BasicStock.State> states = new ArrayList<>(stocks.size());
        for (BasicStock s : stocks) {
            states.add(s.getState());
        }
        return states;
    }

    private ArrayList<String> getStockTickers() {
        final ArrayList<String> tickers = new ArrayList<>(stocks.size());
        for (BasicStock s : stocks) {
            tickers.add(s.getTicker());
        }
        return tickers;
    }

    private ArrayList<Double> getStockPrices() {
        final ArrayList<Double> prices = new ArrayList<>(stocks.size());
        for (BasicStock s : stocks) {
            prices.add(s.getPrice());
        }
        return prices;
    }

    private ArrayList<Double> getStockChangePoints() {
        final ArrayList<Double> changePoints = new ArrayList<>(stocks.size());
        for (BasicStock s : stocks) {
            changePoints.add(s.getChangePoint());
        }
        return changePoints;
    }

    private ArrayList<Double> getStockChangePercents() {
        final ArrayList<Double> changePercents = new ArrayList<>(stocks.size());
        for (BasicStock s : stocks) {
            changePercents.add(s.getChangePercent());
        }
        return changePercents;
    }

    private String getStockTickersAsCSV() {
        return String.join(",", getStockTickers());
    }

    private String getStockStateAndPricesAndChangesAsCSV() {
        final int size = stocks.size();
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

}
