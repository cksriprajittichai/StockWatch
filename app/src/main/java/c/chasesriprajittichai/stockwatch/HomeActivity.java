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

public class HomeActivity extends AppCompatActivity implements FindStockTaskListener {


    private static class DownloadHalfStocksTask extends AsyncTask<String, Integer, Integer> {

        private final int STATUS_GOOD = 0;
        private final int STATUS_IOEXCEPTION = 1;
        private WeakReference<Activity> parentActivity;
        private WeakReference<ArrayList<HalfStock>> halfStocks;
        private WeakReference<RecyclerView> recyclerView;


        private DownloadHalfStocksTask(Activity parentActivity, ArrayList<HalfStock> halfStocks, RecyclerView recyclerView) {
            this.parentActivity = new WeakReference<>(parentActivity);
            this.halfStocks = new WeakReference<>(halfStocks);
            this.recyclerView = new WeakReference<>(recyclerView);
        }


        @Override
        protected synchronized Integer doInBackground(String... tickersArr) {
            ArrayList<String> sortedTickers = new ArrayList<>(tickersArr.length);
            Collections.addAll(sortedTickers, tickersArr);
            Collections.sort(sortedTickers); // Screener table lists alphabetically

            int numStocksTotal = sortedTickers.size();
            int numStocksFinished = 0; // Finished loading and added to halfStocks
            halfStocks.get().clear(); // Remove old HalfStocks
            halfStocks.get().ensureCapacity(numStocksTotal);

            // URL form: <base URL><ticker 1>,<ticker 2>,<ticker 3>,<ticker n>&r=<count at the top of the shown table>
            final String baseUrl = "https://finviz.com/screener.ashx?v=111&t=";

            StringBuilder url = new StringBuilder(numStocksTotal * 5); // Approximate size
            url.append(baseUrl);
            url.append(String.join(",", sortedTickers)); // Append all tickers, separated by commas

            int EXIT_STATUS = STATUS_GOOD;

            int numStocksToFinishThisIteration;
            while (numStocksFinished < numStocksTotal) {
                // If we've completed 60 / 67 stocks, finish only 7 on the last iteration
                if (numStocksTotal - numStocksFinished >= 20) {
                    numStocksToFinishThisIteration = 20;
                } else {
                    numStocksToFinishThisIteration = numStocksTotal - numStocksFinished;
                }

                if (numStocksFinished >= 20) {
                    // URL only needs number at the end if table starts at the 21st stock or higher.
                    /* Need to show the number at the top of the table.
                     * If we've finished 20 / 45 stocks, in the second iteration, the stock at the
                     * top of the table is the 21st stock. */
                    url.append("&r=");
                    url.append(numStocksFinished + 1);
                }

                // URL is now finished. Go to URL and parse and fill halfStocks.
                try {
                    Document doc = Jsoup.connect(url.toString()).get();

                    final String baseSelectorStr = "a[href=\"quote.ashx?t=<TICKER>&ty=c&p=d&b=1\"] span";
                    String curSelectorStr;
                    Elements curVals;
                    int priceNdx, percentNdx;
                    double curPrice, curPercent;
                    String curPerentStr;
                    // Iterate through stocks that we're finishing this iteration
                    for (int i = numStocksFinished; i < numStocksFinished + numStocksToFinishThisIteration; i++) {
                        // Create selector string for current stock
                        curSelectorStr = baseSelectorStr.replace("<TICKER>", sortedTickers.get(i));

                        curVals = doc.select(curSelectorStr);
                        /* curVals could contain three items: P/E, price, price change percent
                         * or curVals could contain two items: price, price change percent */
                        if (curVals.size() == 3) {
                            priceNdx = 1;
                            percentNdx = 2;
                        } else if (curVals.size() == 2) {
                            priceNdx = 0;
                            percentNdx = 1;
                        } else {
                            /* Handle exception better. */
                            halfStocks.get().add(new HalfStock(sortedTickers.get(i), -1, -1));
                            continue;
                        }

                        curPrice = Double.parseDouble(curVals.get(priceNdx).text());
                        curPerentStr = curVals.get(percentNdx).text();
                        // Remove '%' at the end of the percent string. Upper bound is exclusive.
                        curPerentStr = curPerentStr.substring(0, curPerentStr.length() - 1);
                        curPercent = Double.parseDouble(curPerentStr);
                        halfStocks.get().add(new HalfStock(sortedTickers.get(i), curPrice, curPercent));
                    }
                } catch (IOException ioe) {
                    EXIT_STATUS = STATUS_IOEXCEPTION;
                    Log.e("IOException", ioe.getLocalizedMessage());
                }

                numStocksFinished += numStocksToFinishThisIteration;
            }

            return EXIT_STATUS;
        }


        @Override
        protected void onPostExecute(Integer status) {
            if (status == STATUS_GOOD) {
                /* halfStocks is filled in doInBackground() */
                recyclerView.get().setAdapter(new RecyclerHomeAdapter(halfStocks.get(), halfStock -> {
                    Intent intent = new Intent(parentActivity.get(), StockActivity.class);
                    intent.putExtra("Ticker", halfStock.getTicker());
                    intent.putExtra("Is in favorites", true);
                    parentActivity.get().startActivityForResult(intent, 1);
                }));
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
        protected synchronized Boolean doInBackground(String... tickers) {
            ticker = tickers[0];

            // Create URL
            final String baseUrl = "https://finance.yahoo.com/quote/<TICKER>?p=<TICKER>";
            String url = baseUrl.replace("<TICKER>", ticker);

            /* Try to go to the Yahoo Finance stock page of ticker. If ticker exists in Yahoo
             * Finance, this URL leads to the same page that StockActivity gets its data from.
             * Otherwise, this URL leads to a Yahoo Finance stock search page, which shows no
             * results. */
            boolean stockExists = false;
            try {
                Document doc = Jsoup.connect(url).get();

                // Finds an element within a span tag that contains "No results for".
                Element flagElement = doc.selectFirst("span:contains(No results for \'" + ticker + "\')");

                stockExists = (flagElement == null);
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
    //    private String[] start_tickersArr = {"RTN", "BA", "FB", "GE", "GOOGL", "GM", "GS", "HD", "IBM", "JPM", "JNJ", "BAC", "MSFT", "MRK", "AAPL"};
    private String[] start_tickersArr = {"RTN", "BA", "FB", "GE"};
    private ArrayList<HalfStock> halfStocks = new ArrayList<>();
    private ArrayList<String> halfStockTickers = new ArrayList<>();
    private RecyclerView recyclerView;
    private SearchView searchView;
    private SharedPreferences preferences;


    /* Called from FindStockTask.onPostExecute(). */
    @Override
    public void onFindStockTaskCompleted(String ticker, boolean stockExists) {
        if (stockExists) {
            boolean isInFavorites = false;

            // Determine whether ticker isInFavorites
            int compareFlag;
            for (int i = 0; i < halfStockTickers.size(); i++) {
                compareFlag = halfStockTickers.get(i).compareToIgnoreCase(ticker);
                if (compareFlag > 0) {
                    /* HalfStockTickers[i] is the first stock that is > ticker. Ticker hasn't
                     * been found yet, so ticker must not be in halfStockTickers. Use property
                     * that halfStockTickers is sorted. */
                    break;
                } else if (compareFlag == 0) {
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
            Toast.makeText(HomeActivity.this, ticker + " couldn't be found", Toast.LENGTH_SHORT).show();
        }

        /* Enable submit button on SearchView. Submit button was disabled when clicked, to prevent
         * button spamming. */
        searchView.setSubmitButtonEnabled(true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Stock Watch");

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Init recycler view. Set to empty now, will be filled after DownloadHalfStocksTask completes.
        recyclerView = findViewById(R.id.recycler_view_home);
        recyclerView.setAdapter(new RecyclerHomeAdapter(new ArrayList<>(), null)); // Set to empty adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerDivider(this));

        // Starter kit
        Collections.addAll(halfStockTickers, start_tickersArr);
        Collections.sort(halfStockTickers);
    }


    /* Called before onResume(). */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            String ticker = intent.getStringExtra("Ticker");
            boolean stockIsStarred = intent.getBooleanExtra("Is in favorites", false);

            if (stockIsStarred) {
                if (halfStockTickers.isEmpty()) {
                    halfStockTickers.add(ticker);
                } else if (halfStockTickers.get(halfStockTickers.size() - 1).compareToIgnoreCase(ticker) < 0) {
                    /* Using the property that halfStockTickers is sorted, if the last element in
                     * halfStockTickers is < ticker, then halfStockTickers doesn't contain ticker
                     * and ticker should be inserted at the end of halfStockTickers. */
                    halfStockTickers.add(ticker);
                } else {
                    // Insert ticker into halfStockTickers, preserving alphabetical order
                    int compareFlag;
                    for (int i = 0; i < halfStockTickers.size(); i++) {
                        compareFlag = halfStockTickers.get(i).compareToIgnoreCase(ticker);
                        if (compareFlag > 0) {
                            /* HalfStockTickers[i] is the first stock that is > ticker. Ticker hasn't
                             * been found yet, so ticker must not be in halfStockTickers. Use property
                             * that halfStockTickers is sorted. */
                            halfStockTickers.add(i, ticker);
                            break;
                        } else if (compareFlag == 0) {
                            // Ticker is already in favorites. Do nothing.
                            break;
                        }
                    }
                }
            } else {
                // Check if stock is already in favorites. If it is, remove it.
                int compareFlag;
                for (int i = 0; i < halfStockTickers.size(); i++) {
                    compareFlag = halfStockTickers.get(i).compareToIgnoreCase(ticker);
                    if (compareFlag > 0) {
                        // Stock is not in favorites. Use sorted property.
                        break;
                    } else if (compareFlag == 0) {
                        halfStockTickers.remove(i);
                        break;
                    }
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        /* Changes to favorites list can happen one stock at a time. HalfStockTickers is changed
         * when a stock is starred, but a new DownloadHalfStocksTask() must execute in order for
         * halfStocks to be updated to reflect halfStockTickers. */
        if (halfStocks.size() != halfStockTickers.size()) {
            Log.d("Chase", "test 2");
            DownloadHalfStocksTask task = new DownloadHalfStocksTask(this, halfStocks, findViewById(R.id.recycler_view_home));
            String[] halfStockTickersArr = new String[halfStockTickers.size()];
            halfStockTickers.toArray(halfStockTickersArr);
            task.execute(halfStockTickersArr);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
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
                    FindStockTask task = new FindStockTask(HomeActivity.this);
                    task.execute(ticker);
                    /* Prevent spamming of submit button. This also prevents multiple FindStockTasks
                     * from being executed at the same time. Submit button is enabled again in
                     * onFindStockTaskCompleted(). */
                    searchView.setSubmitButtonEnabled(false);
                    /* This leads to FindStockTask which calls onFindStockTaskCompleted()
                     * when completed. */
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

                for (int i = 0; i < halfStocks.size(); i++) {
                    halfStockTickers.set(i, halfStocks.get(i).getTicker());
                }

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount() - 1);
                return true;

            case R.id.sortByPriceMenuItem:
                // Sort by decreasing price
                Comparator<HalfStock> ascendingPriceComparator = Comparator.comparingDouble(HalfStock::getPrice);
                Comparator<HalfStock> descendingPriceComparator = ascendingPriceComparator.reversed();
                halfStocks.sort(descendingPriceComparator);

                for (int i = 0; i < halfStocks.size(); i++) {
                    halfStockTickers.set(i, halfStocks.get(i).getTicker());
                }

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount() - 1);
                return true;

            case R.id.sortByPercentChangeMenuItem:
                // Sort by decreasing magnitude of daily price change percent
                halfStocks.sort((HalfStock a, HalfStock b) -> {
                    // Ignore sign, want stocks with the largest percent change (magnitude only)
                    double aMagnitude = Math.abs(a.getPriceChangePercent());
                    double bMagnitude = Math.abs(b.getPriceChangePercent());

                    return Double.compare(aMagnitude, bMagnitude) * -1; // Flip to get descending
                });

                for (int i = 0; i < halfStocks.size(); i++) {
                    halfStockTickers.set(i, halfStocks.get(i).getTicker());
                }

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;

            case R.id.shuffleMenuItem:
                Collections.shuffle(halfStocks);

                for (int i = 0; i < halfStocks.size(); i++) {
                    halfStockTickers.set(i, halfStocks.get(i).getTicker());
                }

                recyclerView.getAdapter().notifyItemRangeChanged(0, recyclerView.getAdapter().getItemCount());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
