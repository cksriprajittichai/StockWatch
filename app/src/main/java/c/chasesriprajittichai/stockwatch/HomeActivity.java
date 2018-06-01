package c.chasesriprajittichai.stockwatch;

import android.app.Activity;
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
import android.widget.ProgressBar;
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

import static java.lang.Double.parseDouble;

public class HomeActivity extends AppCompatActivity implements FindStockTaskListener, DownloadHalfStocksTaskListener {


    private static class DownloadHalfStocksTask extends AsyncTask<String, Integer, Integer> {

        private final int STATUS_GOOD = 0;
        private final int STATUS_IOEXCEPTION = 1;
        private WeakReference<ArrayList<HalfStock>> halfStocks;
        private WeakReference<RecyclerView> recyclerView;
        private WeakReference<DownloadHalfStocksTaskListener> completionListener;


        private DownloadHalfStocksTask(ArrayList<HalfStock> halfStocks, RecyclerView recyclerView,
                                       DownloadHalfStocksTaskListener completionListener) {
            this.halfStocks = new WeakReference<>(halfStocks);
            this.recyclerView = new WeakReference<>(recyclerView);
            this.completionListener = new WeakReference<>(completionListener);
        }


        @Override
        protected Integer doInBackground(String... tickers) {
            final int numStocksTotal = tickers.length;

            int numStocksFinished = 0; // Finished loading and added to halfStocks
            halfStocks.get().clear(); // Remove old HalfStocks
            halfStocks.get().ensureCapacity(numStocksTotal);

            // URL form: <base URL><ticker 1>,<ticker 2>,<ticker 3>,<ticker n>
            final String baseUrl = "https://www.marketwatch.com/investing/multi?tickers=";

            /* Up to 10 stocks are shown in the MarketWatch view multiple stocks website. The first
             * 10 tickers listed in the URL are shown. Appending more than 10 tickers onto the URL
             * has no effect on the website. */
            StringBuilder url = new StringBuilder(50); // Approximate size

            int EXIT_STATUS = STATUS_GOOD;

            int numStocksToFinishThisIteration, i, websiteNdx;
            double curPrice, curChange, curPercent;
            Elements quoteRoots, prices, priceChangeRoots, priceChanges, priceChangePercents;
            while (numStocksFinished < numStocksTotal) {
                // If we've completed 30 / 37 stocks, finish only 7 on the last iteration
                if (numStocksTotal - numStocksFinished >= 10) {
                    numStocksToFinishThisIteration = 10;
                } else {
                    numStocksToFinishThisIteration = numStocksTotal - numStocksFinished;
                }

                url.append(baseUrl);
                for (i = numStocksFinished; i < numStocksFinished + numStocksToFinishThisIteration; i++) {
                    // Append tickers for stocks that will be created in this iteration
                    url.append(tickers[i]);
                    url.append(',');
                }
                url.deleteCharAt(url.length() - 1); // Delete extra comma

                // URL is now finished. Go to URL and parse and fill halfStocks.
                try {
                    Document doc = Jsoup.connect(url.toString()).get();

                    quoteRoots = doc.select("div[class~=section activeQuote bgQuote (down|up)?]");
                    prices = quoteRoots.select("div[class=lastprice] > div[class=pricewrap] > p[class=data bgLast]");
                    priceChangeRoots = quoteRoots.select("div[class=lastpricedetails] > p[class=lastcolumn data]");
                    priceChanges = priceChangeRoots.select("span[class=bgChange]");
                    priceChangePercents = priceChangeRoots.select("span[class=bgPercentChange]");

                    // Iterate through stocks that we're finishing this iteration
                    for (i = numStocksFinished, websiteNdx = 0; i < numStocksFinished + numStocksToFinishThisIteration; i++, websiteNdx++) {
                        // Remove ',' or '%' that could be in strings
                        curPrice = parseDouble(prices.get(websiteNdx).text().replaceAll("[^0-9|.]", ""));
                        curChange = parseDouble(priceChanges.get(websiteNdx).text().replaceAll("[^0-9|.|\\-]", ""));
                        curPercent = parseDouble(priceChangePercents.get(websiteNdx).text().replaceAll("[^0-9|.|\\-]", ""));
                        halfStocks.get().add(new HalfStock(tickers[i], curPrice, curChange, curPercent));
                    }
                } catch (IOException ioe) {
                    EXIT_STATUS = STATUS_IOEXCEPTION;
                    Log.e("IOException", ioe.getLocalizedMessage());
                }

                numStocksFinished += numStocksToFinishThisIteration;
                url.setLength(0); // Clear URL
            }

            return EXIT_STATUS;
        }


        @Override
        protected void onPostExecute(Integer status) {
            if (status == STATUS_GOOD) {
                completionListener.get().onDownloadHalfStocksTaskCompleted();
            } else if (status == STATUS_IOEXCEPTION) {
                /* Show "No internet connection", or something. */
            }
        }
    }


    private static class FindStockTask extends AsyncTask<String, Integer, Boolean> {

        private WeakReference<FindStockTaskListener> completionListener;
        private String ticker;


        private FindStockTask(FindStockTaskListener completionListener) {
            this.completionListener = new WeakReference<>(completionListener);
        }


        @Override
        protected Boolean doInBackground(String... tickers) {
            ticker = tickers[0];

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


    //    private String[] start_tickersArr = {"BAC", "DIS", "BA", "FB", "GE", "GOOGL", "GM", "GS", "HD",
//            "IBM", "JPM", "JNJ", "CSCO", "CTXS", "ADBE", "AXP", "ANTM", "MSFT", "MRK", "CI", "AAPL", "INTC", "FSLR", "CAT", "RTN", "DKS",
//            "AAL", "DWDP", "DAL", "CVX", "DRYS", "AMD", "AMZN", "NVDA", "T", "TRV", "UTX", "BRK-A", "BRK-B"};
    private ArrayList<HalfStock> halfStocks = new ArrayList<>();
    private RecyclerView recyclerView;
    private SearchView searchView;
    private ProgressBar progressBar;
    private SharedPreferences preferences;


    /* Called from DownloadHalfStocksTask.onPostExecute(). */
    @Override
    public void onDownloadHalfStocksTaskCompleted() {
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
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

            // Go to StockActivity
            Intent intent = new Intent(HomeActivity.this, StockActivity.class);
            intent.putExtra("Ticker", ticker);
            intent.putExtra("Is in favorites", isInFavorites);
            HomeActivity.this.startActivityForResult(intent, 1);
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

        progressBar = findViewById(R.id.progressBar_loadHalfStocks);

        // Init recycler view. Set to empty now, will be filled after DownloadHalfStocksTask completes.
        recyclerView = findViewById(R.id.recyclerView_home);
        recyclerView.setAdapter(new RecyclerHomeAdapter(halfStocks, halfStock -> { // HalfStocks is empty at first
            Intent intent = new Intent(this, StockActivity.class);
            intent.putExtra("Ticker", halfStock.getTicker());
            intent.putExtra("Is in favorites", true);
            startActivityForResult(intent, 1);
        }));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerHomeDivider(this));
    }


    /* Called before onResume(). */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            String ticker = intent.getStringExtra("Ticker");
            boolean stockIsStarred = intent.getBooleanExtra("Is in favorites", false);

            // Check if ticker is already in favorites
            boolean stockIsInFavorites = false;
            String[] tickers = preferences.getString("Tickers CSV", "").split(",");
            for (String tempTicker : tickers) {
                if (tempTicker.equalsIgnoreCase(ticker)) {
                    stockIsInFavorites = true;
                    break;
                }
            }

            if (stockIsStarred) {
                if (!stockIsInFavorites) {
                    // Add ticker to favorites. Add as the first stock in favorites.
                    StringBuilder tickersCSV = new StringBuilder(halfStocks.size() * 5);
                    tickersCSV.append(ticker);
                    tickersCSV.append(',');
                    tickersCSV.append(getHalfStockTickersAsCSV());

                    preferences.edit().putString("Tickers CSV", tickersCSV.toString()).apply();
                }
            } else {
                if (stockIsInFavorites) {
                    // Remove ticker from favorites.
                    StringBuilder tickersCSV = new StringBuilder(halfStocks.size() * 5);
                    for (String t : tickers) {
                        if (!t.equalsIgnoreCase(ticker)) {
                            tickersCSV.append(t);
                            tickersCSV.append(',');
                        }
                    }

                    if (!tickersCSV.toString().isEmpty()) {
                        // Do not put extra comma appended at the end
                        preferences.edit().putString("Tickers CSV",
                                tickersCSV.substring(0, tickersCSV.toString().length() - 1)).apply();
                    } else {
                        // The only ticker in favorites has been removed
                        preferences.edit().putString("Tickers CSV", "").apply();
                    }
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        /** Starter kit */
//        preferences.edit().putString("Tickers CSV", "").apply();
//        preferences.edit().putString("Tickers CSV", "BRK.A,BRK.B,AAL,AAPL,ADBE,AMD,AMZN,ANTM,AXP,BA,BAC,CAT,CI,CSCO,CTXS,CVX,DAL,DIS,DKS,DRYS,DWDP,FB,FSLR,GE,GM,GOOGL,GS,HD,IBM,INTC,JNJ,JPM,MRK,MSFT,NVDA,RTN,T,TRV,UTX").apply();
//        preferences.edit().putString("Tickers CSV", "GOOGL,UTX,RTN,AAL,AAPL,ADBE").apply();
        /** */

        String tickersCSV = preferences.getString("Tickers CSV", "");
        String[] tickers = tickersCSV.split(","); // "".split(",") returns {""}
        // If there are stocks in favorites update halfStocks and recyclerView.
        if (!tickers[0].equals("")) {
            DownloadHalfStocksTask task = new DownloadHalfStocksTask(halfStocks, findViewById(R.id.recyclerView_home), this);
            task.execute(tickers);
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE); // Make visible when task is completed
        } else if (!halfStocks.isEmpty()) {
            /* Tickers CSV preference is an empty string, meaning that there should be no stocks in
             * favorites. Stocks can only be removed one at a time, so there must have only been
             * one stock remaining in favorites. Remove it. */
            halfStocks.clear(); // Same effect as halfStocks.remove(0)
            recyclerView.getAdapter().notifyItemRemoved(0);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        preferences.edit().putString("Tickers CSV", getHalfStockTickersAsCSV()).apply();
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
                    if (!Character.isLetterOrDigit(c) && c != '.' && c != '-') {
                        isValidTicker = false;
                        break;
                    }
                }

                if (isValidTicker) {
                    /* The thread executing the FindStockTask will call onFindStockTaskCompleted()
                     * when completed. */
                    FindStockTask task = new FindStockTask(HomeActivity.this);
                    task.execute(ticker);

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
                Comparator<HalfStock> tickerComparator = Comparator.comparing(HalfStock::getTicker);
                halfStocks.sort(tickerComparator);

                preferences.edit().putString("Tickers CSV", getHalfStockTickersAsCSV()).apply();

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;

            case R.id.sortByPriceMenuItem:
                // Sort by decreasing price
                Comparator<HalfStock> ascendingPriceComparator = Comparator.comparingDouble(HalfStock::getPrice);
                Comparator<HalfStock> descendingPriceComparator = ascendingPriceComparator.reversed();
                halfStocks.sort(descendingPriceComparator);

                preferences.edit().putString("Tickers CSV", getHalfStockTickersAsCSV()).apply();

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;

            case R.id.sortByPercentChangeMenuItem:
                // Sort by decreasing magnitude of daily price change percent
                halfStocks.sort((HalfStock a, HalfStock b) -> {
                    // Ignore sign, want stocks with the largest percent change (magnitude only)
                    double aMagnitude = Math.abs(a.getPriceChangePercent());
                    double bMagnitude = Math.abs(b.getPriceChangePercent());

                    return Double.compare(aMagnitude, bMagnitude) * -1; // Flip to get descending
                });

                preferences.edit().putString("Tickers CSV", getHalfStockTickersAsCSV()).apply();

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;

            case R.id.shuffleMenuItem:
                Collections.shuffle(halfStocks);

                preferences.getString("Tickers CSV", "");
                preferences.edit().putString("Tickers CSV", getHalfStockTickersAsCSV()).apply();

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private ArrayList<String> getHalfStockTickers() {
        ArrayList<String> tickers = new ArrayList<>(halfStocks.size());
        for (HalfStock halfStock : halfStocks) {
            tickers.add(halfStock.getTicker());
        }
        return tickers;
    }


    private String getHalfStockTickersAsCSV() {
        return String.join(",", getHalfStockTickers());
    }

}
