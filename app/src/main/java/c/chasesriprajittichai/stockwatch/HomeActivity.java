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
    private RequestQueue requestQueue;
    private final HashMap<String, BasicStock> tickerToStockMap = new HashMap<>();

    /* Called from FindStockTask.onPostExecute(). */
    @Override
    public void onFindStockTaskCompleted(String ticker, boolean stockExists) {
        if (stockExists) {
            boolean isInFavorites = false;

            // Determine whether ticker isInFavorites already
            String[] tickers = mpreferences.getString("Tickers CSV", "").split(",");
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
            msearchView.setQuery("", false);
            Toast.makeText(HomeActivity.this, ticker + " couldn't be found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Stock Watch");

        requestQueue = Volley.newRequestQueue(this);

        mpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        /* Starter kit */
//        mpreferences.edit().putString("Tickers CSV", "").apply();
//        mpreferences.edit().putString("Data CSV", "").apply();
//        fillPreferencesWithLargeRandomData(1000);

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

                // Make mstocks and tickertoStockMap point to the same BasicStock objects
                mstocks.add(curStock);
                tickerToStockMap.put(curTicker, curStock);
            }
        }

        mrecyclerView = findViewById(R.id.recyclerView_home);
        mrecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mrecyclerView.addItemDecoration(new RecyclerHomeDivider(this));
        mrecyclerView.setAdapter(new RecyclerHomeAdapter(mstocks, basicStock -> {
            Intent intent = new Intent(this, IndividualStockActivity.class);
            intent.putExtra("Ticker", basicStock.getTicker());
            intent.putExtra("Is in favorites", true);
            startActivity(intent);
        }));
        setUpItemTouchHelper();
    }

    private void setUpItemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleItemTouchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            // Minimize the amount of object allocation done in onChildDraw()
            final Drawable background = new ColorDrawable(Color.RED);
            final Drawable garbaceIcon = ContextCompat.getDrawable(HomeActivity.this, R.drawable.ic_delete_black_24dp);
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

                removeStockFromPreferences(mstocks.get(position).getTicker());
                tickerToStockMap.remove(mstocks.get(position).getTicker());
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
                int intrinsicWidth = garbaceIcon.getIntrinsicWidth();
                int intrinsicHeight = garbaceIcon.getIntrinsicWidth();

                int xMarkLeft = itemView.getRight() - garbageMargin - intrinsicWidth;
                int xMarkRight = itemView.getRight() - garbageMargin;
                int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int xMarkBottom = xMarkTop + intrinsicHeight;
                garbaceIcon.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

                garbaceIcon.draw(c);

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
     * stocks. The tickers from each partition's stocks are used to build a URL for the Market Watch
     * multiple-stock-website. The URL is then sent into a Volley request queue. The parsing of the
     * HTML retrieved from the websites and updating of variables happens in onResponse().
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
            requestQueue.add(stringRequest);

            numStocksUpdated += numStocksToUpdateThisIteration;
            url.setLength(0); // Clear URL
        }
    }

    @Override
    public void onResponse(String response) {
        BasicStock curStock, firstStockToUpdate = null;
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

            /* mstocks points to the same BasicStock objects that tickerToStockMap has pointers to.
             * So by updating curStock, which points to a BasicStock in tickerToStockMap, we are
             * also updating mstocks.. */
            curStock = tickerToStockMap.get(curTicker);
            curStock.setState(curState);
            curStock.setPrice(curPrice);
            curStock.setChangePoint(curChangePoint);
            curStock.setChangePercent(curChangePercent);

            if (i == 0) {
                firstStockToUpdate = curStock;
            }
        }

        /* Update mrecyclerView. Stocks that are passed into
         * this function are in adjacent positions in mstocks. */
        mrecyclerView.getAdapter().notifyItemRangeChanged(
                mstocks.indexOf(firstStockToUpdate), numStocksToUpdate);
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
        switch (item.getItemId()) {
            case R.id.sortAlphabeticallyMenuItem:
                Comparator<BasicStock> tickerComparator = Comparator.comparing(BasicStock::getTicker);
                mstocks.sort(tickerComparator);

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
                return true;
            case R.id.sortByPriceMenuItem:
                // Sort by decreasing price
                Comparator<BasicStock> ascendingPriceComparator = Comparator.comparingDouble(BasicStock::getPrice);
                Comparator<BasicStock> descendingPriceComparator = ascendingPriceComparator.reversed();
                mstocks.sort(descendingPriceComparator);

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

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
                return true;
            case R.id.shuffleMenuItem:
                Collections.shuffle(mstocks);

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
                return true;
            case R.id.flipListMenuItem:
                Collections.reverse(mstocks);

                mrecyclerView.getAdapter().notifyItemRangeChanged(0, mrecyclerView.getAdapter().getItemCount());
            default:
                return super.onOptionsItemSelected(item);
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
    private void fillPreferencesWithLargeRandomData(int size) {
        String tickersStr = "PIH,PIHPP,TURN,FLWS,FCCY,SRCE,VNET,TWOU,JOBS,CAFD,EGHT,AVHI,SHLM,AAON,ABAX,ABEO,ABEOW,ABIL,ABMD,ABLX,ABP,AXAS,ACIU,ACIA,ACTG,ACHC,ACAD,ACST,AXDX,XLRN,ANCX,ARAY,ACRX,ACER,ACET,AKAO,ACHV,ACHN,ACIW,ACRS,ACMR,ACNB,ACOR,ATVI,ACXM,ADMS,ADMP,ADAP,ADUS,AEY,IOTS,ADMA,ADBE,ADOM,ADTN,ADRO,ADES,AEIS,AMD,ADXS,ADXSW,ADVM,ACT,AEGN,AGLE,AEHR,AMTX,AERI,AVAV,AEZS,AEMD,GNMX,AFMD,AGEN,AGRX,AGYS,AGIO,AGMH,AGNC,AGNCB,AGNCN,AGFS,AGFSW,ALRN,AIMT,AIRT,ATSG,AIRG,AMCN,AKAM,AKTX,AKCA,AKBA,AKER,AKRX,AKTS,ALRM,ALSK,ALBO,ABDC,ALDR,ALDX,ALXN,ALCO,ALGN,ALIM,ALJJ,ALKS,ABTX,ALGT,ALNA,AMMA,ARLP,AHPI,AMOT,ALQA,ALLT,MDRX,ALNY,AOSL,GOOG,GOOGL,SMCP,ATEC,ALPN,SWIN,AMR,AMRWW,AABA,ALTR,ALT,ASPS,AIMC,AMAG,AMRN,AMRK,AMZN,AMBC,AMBCW,AMBA,AMCX,DOX,AMDA,AMED,UHAL,AMRH,AMRHW,ATAX,AMOV,AAL,ACSF,AETI,AMNB,ANAT,AOBC,APEI,ARII,AMRB,AMSWA,AMSC,AMWD,CRMT,ABCB,AMSF,ASRV,ASRVP,ATLO,AMGN,FOLD,AMKR,AMPH,IBUY,ASYS,AFSI,AMRS,ADI,ALOG,ANAB,AVXL,ANCB,ANGI,ANGO,ANIP,ANIK,ANSS,ATRS,ANTH,APLS,APOG,APEN,AINV,AMEH,APPF,APPN,AAPL,ARCI,APDN,APDNW,AGTC,AMAT,AAOI,AREX,APTI,APRI,APVO,APTO,AQMS,AQB,AQXP,ARDM,ARLZ,PETX,ABUS,ARCW,ABIO,RKDA,ARCB,ACGL,ACGLO,ACGLP,FUV,ARCT,ARDX,ARNA,ARCC,ARGX,ARKR,ARMO,ARTX,ARQL,ARRY,ARRS,DWCR,DWAT,AROW,ARWR,ASNS,ARTNA,ARTW,ASNA,ASND,ASCMA,APWC,ASLN,ASML,ASPU,AZPN,ASMB,ASFI,ASTE,ATRO,ALOT,ASTC,ASUR,ASV,ATAI,ATRA,ATHN,ATNX,ATHX,ATAC,ATACR,ATACU,AAME,ACBI,AY,ATLC,AAWW,AFH,AFHBL,TEAM,ATNI,ATOM,ATOS,ATRC,ATRI,ATIS,ATISW,ATTU,LIFE,AUBN,BOLD,AUDC,AUPH,EARS,ADSK,ADP,AUTO,AVDL,ATXI,AVEO,AVNW,CDMO,CDMOP,AVID,AVGR,CAR,AHPA,AHPAU,AHPAW,AVT,AWRE,ACLS,AXGN,AAXN,AXON,AXSM,AXTI,AYTU,AZRX,BCOM,RILY,RILYG,RILYH,RILYL,RILYZ,BOSC,BIDU,BCPC,BWINA,BWINB,BLDP,BANF,BANFP,BCTF,BAND,BOCH,BMRC,BMLP,BKSC,BOTJ,OZRK,BFIN,BWFG,BANR,BZUN,DFVL,DFVS,DLBL,DLBS,DTUL,DTUS,DTYL,DTYS,FLAT,STPP,TAPR,BHAC,BHACR,BHACU,BHACW,BBSI,BSET,BCML,BCBP,BECN,BBGI,BBBY,BGNE,BELFA,BELFB,BLPH,BLCM,BNCL,BNFT,BNTC,BNTCW,BYSI,BGCP,BGFV,BRPA,BRPAR,BRPAU,BRPAW,BILI,BASI,ORPN,BIOC,BCRX,BDSI,BFRA,BIIB,BHTG,BKYI,BIOL,BLFS,BLRX,BMRN,BMRA,BVXV,BVXVW,BPTH,BIOS,BSTC,BSPM,TECH,BEAT,BTAI,BCAC,BCACR,BCACU,BCACW,BJRI,BBOX,BRAC,BRACR,BRACU,BRACW,BLKB,HAWK,BL,BKCC,BLNK,BLNKW,BLMN,BCOR,BLBD,BHBK,BLUE,BKEP,BKEPP,BPMC,ITEQ,BMCH,BOFI,BOFIL,WIFI,BOJA,BOKF,BOKFL,BNSO,BKNG,BRQS,BOMN,BPFH,BPFHP,BPFHW,EPAY,BOXL,BCLI,BVNSC,BDGE,BLIN,BWB,BRID,BCOV,BHF,AVGO,BVSN,BYFC,BWEN,BPY,BRKL,BRKS,BRKR,BMTC,BLMT,BSQR,BLDR,BFST,CFFI,CHRW,CA,CCMP,CDNS,CDZI,CZR,CSTE,PRSS,CLBS,CHY,CHI,CCD,CHW,CGO,CSQ,CAMP,CVGW,CALA,CALM,CLMT,CRUSC,CLXT,ABCD,CATC,CAC,CAMT,CSIQ,CGIX,CPHC,CPLA,CCBG,CPLP,CSWC,CSWCL,CPTA,CPTAG,CPTAL,CFFN,CAPR,CSTR,CPST,CARA,CBLK,CARB,CSII,CDLX,CATM,CDNA,CECO,CTRE,CARG,CARO,CART,CRZO,TAST,CARV,CASM,CASA,CWST,CASY,CASI,CASS,CATB,CBIO,CPRX,CATS,CATY,CATYW,CGVIC,CIVEC,CVCO,CAVM,CBFV,CBAK,CBOE,CBTX,CDK,CDTI,CDW,CECE,CELC,CELG,CELGZ,CLDX,APOP,APOPW,CLRB,CLRBW,CLRBZ,CLLS,CBMG,CLSN,CELH,CYAD,CETX,CETXP,CETXW,CDEV,CSFL,CETV,CFBK,CENT,CENTA,CVCY,CENX,CNBKA,CNTY,CRNT,CERC,CERCW,CERN,CERS,KOOL,CEVA,CSBR,CYOU,BURG,CTHR,GTLS,CHTR,CHFN,CHKP,CHEK,CHEKW,CHEKZ,CKPT,CEMI,CHFC,CCXI,CHMG,CHKE,CHFS,CHMA,CSSE,PLCE,CMRX,CADC,CALI,CAAS,CBPO,CCCL,CCCR,CCRC,JRJC,HGSH,CIFS,CJJD,CLDC,HTHT,CHNR,CREG,CNTF,CXDC,CCIH,CNET,IMOS,CDXC,CHSCL,CHSCM,CHSCN,CHSCO,CHSCP,CHDN,CHUY,CDTX,CMCT,CMCTP,CMPR,CINF,CIDM,CTAS,CRUS,CSCO,CTRN,CTXR,CTXRW,CZNC,CZWI,CZFC,CIZN,CTXS,CHCO,CIVB,CIVBP,CLAR,CLNE,CACG,YLDE,LRGE,CLFD,CLRO,CLSD,CLIR,CLIRW,CMTA,CBLI,CLVS,CLPS,CMFN,CMSS,CMSSR,CMSSU,CMSSW,CME,CCNE,CWAY,COBZ,COKE,COCP,CODA,CDXS,CODX,CVLY,JVA,CCOI,CGNX,CTSH,CWBR,COHR,CHRS,COHU,CLCT,COLL,CIGI,CLGN,CBAN,COLB,CLBK,COLM,CMCO,CMCSA,CBSH,CBSHP,CVGI,COMM,JCS,ESXB,CFBI,CYHHZ,CTBI,CWBC,CVLT,CGEN,CPSI,CTG,SCOR,CHCI,CMTL,CNAT,CNCE,CXRX,CDOR,CFMS,CNFR,CNMD,CTWS,CNOB,CONN,CNSL,CWCO,CNAC,CNACR,CNACU,CNACW,ROAD,CPSS,CFRX,CTRV,CTRL,CVON,CVONW,CPRT,CRBP,CORT,CORE,CORI,CSOD,CORV,CRVL,CRVS,CSGP,COST,CPAH,ICBK,COUP,CVTI,COWN,COWNZ,PMTS,CPSH,CRAI,CBRL,BREW,CRAY,CACC,DGLD,DSLV,GLDI,SLVO,TVIX,TVIZ,UGLD,USLV,USOI,VIIX,VIIZ,ZIV,CREE,CRESY,CRSP,CRTO,CROX,CRON,CCRN,CRWS,CYRX,CYRXW,CSGS,CCLP,CSPI,CSWI,CSX,CTIC,CTIB,CTRP,CUE,CUI,CPIX,CRIS,CUTR,CVBF,CVV,CYAN,CYBR,CYBE,CYCC,CYCCP,CBAY,CY,CYRN,CONE,CYTK,CTMX,CYTX,CYTXW,CYTXZ,CTSO,CYTR,DJCO,DAKT,DARE,DRIO,DRIOW,DZSI,DSKE,DSKEW,DAIO,DWCH,PLAY,DTEA,DFNL,DINT,DUSA,DWLD,DWSN,DBVT,DCPH,DFRG,TACO,TACOW,DMPI,DELT,DNLI,DENN,XRAY,DEPO,DERM,DEST,DXLG,DSWL,DTRM,DXCM,DFBH,DFBHU,DFBHW,DHXM,DHIL,FANG,DCIX,DRNA,DFBG,DFFN,DGII,DMRC,DRAD,DGLY,APPS,DCOM,DIOD,DISCA,DISCB,DISCK,DISH,DVCR,SAUC,DLHC,BOOM,DNBF,DOCU,DOGZ,DLTR,DLPN,DLPNW,DGICA,DGICB,DMLP,DORM,DOVA,LYL,DOTA,DOTAR,DOTAU,DOTAW,DBX,DCAR,DRYS,DSPG,DLTH,DNKN,DRRX,DXPE,DYSL,DYNT,DVAX,ETFC,SSP,EBMT,EGBN,EGLE,EFBI,EGRX,EWBC,EACQ,EACQU,EACQW,EML,EAST,EASTW,EVGBC,EVSTC,EVFTC,EVLMC,OKDCC,EBAY,EBAYL,EBIX,ELON,ECHO,SATS,EEI,ESES,EDAP,EDGE,EDGW,EDIT,EDUC,EGAN,EGLT,EHTH,EIGR,EKSO,LOCO,EMITF,ESLT,ERI,ESIO,EA,EFII,ELSE,ELEC,ELECU,ELECW,ESBK,ELOX,ELTK,EMCI,EMCF,EMKR,EMMS,NYNY,ENTA,ECPG,WIRE,ENDP,ECYT,ELGX,NDRA,NDRAW,EIGI,WATT,EFOI,ERII,EGC,ENG,ENPH,ESGR,ENFC,ENTG,EBTC,EFSC,EPZM,PLUS,EQIX,EQFN,EQBK,ERIC,ERIE,ERYP,ESCA,ESPR,ESQ,ESSA,EPIX,ESND,ESTR,ESTRW,VBND,VUSE,VIDI,ETSY,CLWT,EDRY,EEFT,ESEA,EVLO,EVBG,EVK,MRAM,EVLV,EVOP,EVFM,EVGN,EVOK,EOLS,EVOL,EXAS,FLAG,ROBO,XELA,EXEL,EXFO,EXLS,EXPI,EXPE,EXPD,EXPO,ESRX,XOG,EXTR,EYEG,EYEGW,EYEN,EYPT,EZPW,FFIV,FB,DAVE,FANH,FARM,FMAO,FFKT,FMNB,FAMI,FARO,FAST,FAT,FATE,FBSS,FCRE,FSAC,FSACU,FSACW,FNHC,FENC,GSM,FFBW,FCSC,FGEN,FDBC,ONEQ,LION,FDUS,FDUSL,FRGI,FITB,FITBI,FNGN,FISI,FNSR,FNJN,FNTE,FNTEU,FNTEW,FEYE,FBNC,FNLC,FRBA,BUSE,FBIZ,FCAP,FCFS,FCBP,FCNCA,FCBC,FCCO,FBNK,FDEF,FFBC,FFBCW,FFIN,THFF,FFNW,FFWM,FGBI,FHB,INBK,INBKL,FIBK,FRME,FMBH,FMBI,FNWB,FSFG,FSLR,FAAR,FPA,BICK,FBZ,FTHI,FCAL,FCAN,FTCS,FCEF,FCA,SKYY,RNDM,FDT,FDTS,FVC,FV,IFV,DWPP,DALI,FEM,RNEM,FEMB,FEMS,FTSM,FEP,FEUZ,FGM,FTGC,FTLB,HYLS,FHK,NFTY,FTAG,FTRI,LEGR,FPXI,YDIV,FJP,FEX,FTC,RNLC,FTA,FLN,LMBS,FMB,FMK,FNX,FNY,RNMC,FNK,FAD,FAB,MDIV,MCEF,FMHI,QABA,ROBT,FTXO,QCLN,GRID,CIBR,FTXG,CARZ,FTXN,FTXH,FTXD,FTXL,FONE,TDIV,FTXR,QQEW,QQXT,QTEC,AIRR,QINC,RDVY,RFAP,RFDI,RFEM,RFEU,FTSL,FYX,FYC,RNSC,FYT,SDVY,FKO,FCVT,FDIV,FSZ,FIXD,TUSA,FKU,RNDV,FUNC,FUSB,SVVC,FSV,FISV,FIVE,FPRX,FVE,FIVN,FLEX,FLKS,FLXN,SKOR,LKOR,MBSD,ASET,ESGG,ESG,QLC,FPAY,FLXS,FLIR,FLNT,FLDM,FFIC,FNBG,FNCB,FOMX,FONR,FSCT,FRSX,FORM,FORTY,FORR,FRTA,FTNT,FBIO,FBIOP,FWRD,FORD,FWP,FOSL,FMI,FOXF,FRAN,FELE,FRED,RAIL,FEIM,FRPT,FTEO,FTR,FTRPR,FRPH,FSBW,FSBC,FTD,FTEK,FCEL,FLGT,FORK,FLL,FULT,FNKO,FSNN,FTFT,FFHL,WILC,GTHX,FOANC,GRBIC,MOGLC,GAIA,GLPG,GALT,GLMD,GLPI,GPIC,GRMN,GARS,GLIBA,GLIBP,GDS,GEMP,GENC,GFN,GFNCP,GFNSL,GENE,GNUS,GNMK,GNCA,GHDX,GNPX,GNTX,THRM,GEOS,GABC,GERN,GEVO,ROCK,GIGM,GIII,GILT,GILD,GBCI,GLAD,GLADN,GOOD,GOODM,GOODO,GOODP,GAIN,GAINM,GAINN,GAINO,LAND,LANDP,GLBZ,GBT,ENT,GBLI,GBLIL,GBLIZ,SELF,GWRS,DRIV,KRMA,FINX,AIQ,BFIT,SNSR,LNGR,MILN,EFAS,QQQC,BOTZ,CATH,SOCL,ALTY,SRET,YLCO,GLBS,GLUU,GLYC,GOGO,GLNG,GMLP,GMLPP,DNJR,GDEN,GOGL,GBDC,GTIM,GSHD,GPRO,GPAQ,GPAQU,GPAQW,GSHT,GSHTU,GSHTW,GOV,GOVNI,LOPE,GRVY,GECC,GECCL,GECCM,GEC,GLDD,GSBC,GNBC,GRBK,GPP,GPRE,GCBC,GLRE,GSKY,GSUM,GRIF,GRFS,GRPN,OMAB,GGAL,GVP,GSIT,GSVC,GTXI,GTYH,GTYHU,GTYHW,GBNK,GNTY,GFED,GIFI,GURE,GPOR,GWPH,GWGH,GYRO,HEES,HLG,HNRG,HALL,HALO,HBK,HLNE,HJLI,HJLIW,HWC,HWCPL,HAFC,HQCL,HONE,HLIT,HFGIC,HBIO,HCAP,HCAPZ,HAS,HA,HCOM,HWKN,HWBK,HYAC,HYACU,HYACW,HAYN,HDS,HIIQ,HCSG,HQY,HSTM,HTLD,HTLF,HTBX,HEBT,HSII,HELE,HMNY,HSDT,HMTV,HNNA,HSIC,HTBK,HFWA,HCCI,MLHR,HRTX,HSKA,HX,HIBB,SNLN,HPJ,HIHO,HIMX,HIFS,HSGX,HMNF,HMSY,HOLI,HOLX,HBCP,HOMB,HFBL,HMST,HMTA,HTBI,FIXX,HOFT,HOPE,HFBC,HBNC,HZNP,HRZN,DAX,QYLD,HDP,HPT,TWNK,TWNKW,HMHC,HWCC,HOVNP,HBMD,HTGM,HUBG,HSON,HDSN,HUNT,HUNTU,HUNTW,HBAN,HBANN,HBANO,HURC,HURN,HCM,HBP,HVBC,HYGS,IDSY,IAC,IAM,IAMXR,IAMXW,IBKC,IBKCO,IBKCP,ICAD,IEP,ICCH,ICFI,ICHR,ICLK,ICLR,ICON,ICUI,IPWR,INVE,IDRA,IDXX,IESC,IROQ,IFMK,INFO,IIVI,KANG,IKNX,ILG,ILMN,ISNS,IMMR,ICCC,IMDZ,IMNP,IMGN,IMMU,IMRN,IMRNW,IMMP,IMPV,PI,IMMY,IMV,INCY,INDB,IBCP,IBTX,INDU,INDUU,INDUW,ILPT,IDSA,INFN,INFI,IPCC,IFRX,III,IFON,IEA,IEAWW,IMKTA,INWK,INOD,IPHS,IOSP,INNT,ISSC,INVA,INGN,INOV,INO,INPX,INSG,NSIT,ISIG,INSM,INSE,IIIN,PODD,INSY,NTEC,IART,IDTI,IMTE,INTC,NTLA,IPCI,IPAR,IBKR,ICPT,IDCC,TILE,LINK,IMI,INAP,IBOC,ISCA,IGLD,IIJI,IDXG,XENT,INTX,IVAC,INTL,ITCI,IIN,INTU,ISRG,PLW,ADRA,ADRD,ADRE,ADRU,PKW,PFM,PYZ,PEZ,PSL,PIZ,PIE,PXI,PFI,PTH,PRN,DWLV,PDP,DWAQ,DWAS,DWIN,DWTR,PTF,PUI,IDLB,PRFZ,PAGG,PSAU,PIO,PGJ,PEY,IPKW,PID,KBWB,KBWD,KBWY,KBWP,KBWR,LDRI,LALT,PNQI,PDBC,QQQ,USLB,PSCD,PSCC,PSCE,PSCF,PSCH,PSCI,PSCT,PSCM,PSCU,VRIG,PHO,ISTR,ISBC,ITIC,NVIV,IVTY,IONS,IOVA,IPAS,IPGP,IPIC,CLRG,CSML,IQ,IRMD,IRTC,IRIX,IRDM,IRDMB,IRBT,IRWD,IRCP,PMPT,SLQD,CSJ,ISHG,SHY,TLT,IEI,IEF,AIA,COMT,ISTB,IXUS,IUSG,IUSV,IUSB,HEWG,SUSB,SUSC,XT,FALN,IFEU,IFGL,IGF,GNMA,HYXE,CIU,IGOV,EMB,MBB,JKI,ACWX,ACWI,AAXJ,EWZS,MCHI,ESGD,SCZ,ESGE,EEMA,EMXC,EUFN,IEUS,RING,MPCT,ENZL,QAT,TUR,UAE,ESGU,IBB,SOXX,AMCA,EMIF,ICLN,WOOD,INDY,IJT,DVY,SHV,CRED,PFF,ISRL,ITI,ITRM,ITRI,ITRN,ITUS,IVENC,IVFGC,IVFVC,IZEA,JJSF,MAYS,JBHT,JCOM,JASO,JKHY,JACK,JXSB,JAGX,JAKK,JMBA,JRVR,JSML,JSMD,JASN,JASNW,JAZZ,JD,JSYN,JSYNR,JSYNU,JSYNW,JRSH,JBLU,JTPY,JCTCF,JMU,JBSS,JOUT,JNCE,JNP,KTWO,KALU,KALA,KALV,KMDA,KNDI,KPTI,KAAC,KAACU,KAACW,KZIA,KBLM,KBLMR,KBLMU,KBLMW,KBSF,KCAP,KCAPL,KRNY,KELYA,KELYB,KMPH,KFFB,KERX,KEQU,KTCC,KFRC,KE,KBAL,KIN,KGJI,KINS,KONE,KNSA,KNSL,KIRK,KTOV,KTOVW,KLAC,KLXI,KONA,KOPN,KRNT,KOSS,KWEB,KTOS,KRYS,KLIC,KURA,KVHI,FSTR,LJPC,LSBK,LBAI,LKFN,LAKE,LRCX,LAMR,LANC,LCA,LCAHU,LCAHW,LNDC,LARK,LMRK,LMRKN,LMRKO,LMRKP,LE,LSTR,LNTH,LTRX,LSCC,LAUR,LAWS,LAYN,LAZY,LCNB,LBIX,LPTX,LGCY,LGCYO,LGCYP,LTXB,DDBI,EDBI,INFR,LVHD,SQLV,UDBI,LACQ,LACQU,LACQW,LMAT,TREE,LEVL,LXRX,LX,LGIH,LHCG,LLIT,LBRDA,LBRDK,LEXEA,LEXEB,LBTYA,LBTYB,LBTYK,LILA,LILAK,BATRA,BATRK,FWONA,FWONK,LSXMA,LSXMB,LSXMK,TAX,LTRPA,LTRPB,LPNT,LCUT,LFVN,LWAY,LGND,LTBR,LPTH,LLEX,LMB,LLNW,LMNR,LINC,LECO,LIND,LINDW,LPCN,LQDT,LFUS,LIVN,LOB,LIVE,LPSN,LIVX,LKQ,LMFA,LMFAW,LOGI,LOGM,CNCR,LONE,LOOP,LORL,LOXO,LPLA,LRAD,LYTS,LULU,LITE,LMNX,LUNA,LBC,MBTF,MACQ,MACQU,MACQW,MBVX,MCBC,MFNC,MTSI,MGNX,MDGL,MAGS,MGLN,MGIC,CALL,MNGA,MGYR,MHLD,MMYT,MBUU,MLVF,MAMS,TUSK,RPIBC,MANH,LOAN,MNTX,MTEX,MNKD,MANT,MARA,MCHX,MARPS,MRNS,MKTX,MRLN,MAR,MBII,MRTN,MMLP,MRVL,MASI,MTCH,MTLS,MPAC,MPACU,MPACW,MTRX,MAT,MATR,MATW,MXIM,MXWL,MZOR,MBFI,MBFIO,MCFT,MGRC,MDCA,MFIN,MFINL,MTBC,MTBCP,MNOV,MDSO,MDGS,MDWD,MEDP,MEIP,MLCO,MLNT,MLNX,MELR,MNLO,MTSL,MELI,MBWM,MERC,MBIN,MRCY,EBSB,MRBK,VIVO,MMSI,MACK,MRSN,MRUS,MLAB,MESO,CASH,MEOH,MGEE,MGPI,MBOT,MCHP,MU,MICT,MSFT,MSTR,MVIS,MPB,MTP,MCEP,MBCN,MSEX,MSBI,MOFG,MIME,MDXG,MNDO,MB,NERV,MGEN,MRTX,MSON,MIND,MINDP,MITK,MITL,MKSI,MMAC,MINI,MOBL,MMDM,MMDMR,MMDMU,MMDMW,MOGO,MTEM,MBRX,MNTA,MOMO,MKGI,MCRI,MDLZ,MGI,MDB,MPWR,TYPE,MNRO,MRCC,MNST,MORN,MOR,MOSY,MTFB,MTFBW,MPAA,MOTS,MPVD,MOXC,MSBF,MTEC,MTECU,MTECW,MTGE,MTGEP,MTSC,MUDS,MUDSU,MUDSW,LABL,MBIO,MFSF,MVBF,MYSZ,MYL,MYND,MYNDW,MYOK,MYOS,MYRG,MYGN,NBRV,NAKD,NNDM,NANO,NSTG,NAOV,NH,NK,NSSC,NDAQ,NTRA,NATH,NAUH,NKSH,FIZZ,NCMI,NCOM,NESR,NESRW,NGHC,NGHCN,NGHCO,NGHCP,NGHCZ,NHLD,NHLDW,NATI,NRC,NSEC,EYE,NWLI,NAII,NHTC,NATR,BABY,JSM,NAVI,NBTB,NCSM,NEBU,NEBUU,NEBUW,NKTR,NMRD,NEOG,NEO,NEON,NEOS,NVCN,NEPT,UEPS,NETE,NTAP,NTES,NFLX,NTGR,NLST,NTCT,NTWK,CUR,NBIX,NURO,NUROW,NTRP,NBEV,NYMT,NYMTN,NYMTO,NYMTP,NEWA,NLNK,NMRK,NWS,NWSA,NEWT,NEWTI,NEWTZ,NXEO,NXEOU,NXEOW,NXST,NEXT,NFEC,NODK,EGOV,NICE,NICK,NCBS,NITE,NIHD,LASR,NMIH,NNBR,NDLS,NDSN,NSYS,NBN,NTIC,NTRS,NTRSP,NFBK,NRIM,NWBI,NWPX,NCLH,NWFL,NVFY,NVMI,NOVN,NOVT,NVAX,NVLN,NVCR,NVMM,NVUS,NUAN,NCNA,NTNX,NTRI,NUVA,NVTR,QQQX,NVEE,NVEC,NVDA,NXPI,NXTM,NXTD,NXTDW,NYMX,OIIM,OVLY,OCSL,OCSLL,OCSI,OASM,OBLN,OBSV,OBCI,OPTT,ORIG,OCFC,OCLR,OFED,OCUL,ODT,OMEX,ODP,OFS,OFSSL,OHAI,OVBC,OHRP,OKTA,ODFL,OLBK,ONB,OPOF,OSBC,OSBCP,OLLI,ZEUS,OFLX,OMER,OMCL,ON,OTIV,ONS,ONSIW,ONCY,OMED,ONTX,ONTXW,ONCS,OHGI,OSS,OPBK,OTEX,OPES,OPESU,OPESW,OPGN,OPGNW,OPHT,OPNT,OPK,OBAS,OCC,OPHC,OPTN,OPB,ORMP,OSUR,ORBC,ORBK,ORLY,ONVO,ORGS,SEED,OBNK,OESX,ORIT,ORRF,OFIX,KIDS,OSIS,OSPR,OSPRU,OSPRW,OSN,OTEL,OTIC,OTTW,OTTR,OVAS,OSTK,OVID,OXBR,OXBRW,OXFD,OXLC,OXLCM,OXLCO,OXSQ,OXSQL,PFIN,PTSI,PCAR,VETS,PACB,PEIX,PMBC,PPBI,PCRX,PACW,PTIE,PAAS,PANL,PZZA,FRSH,PRTK,PCYG,PKBK,PRKR,PKOH,PTNR,PBHC,PATK,PNBK,PATI,PEGI,PDCO,PTEN,PAVM,PAVMW,PAVMZ,PAYX,PCTY,PYDS,PYPL,PBBI,CNXN,PCMI,PCSB,PCTI,PDCE,PDFS,PDLI,PDLB,PDVW,SKIS,PGC,PEGA,PENN,PVAC,PFLT,PNNT,PWOD,WRLS,WRLSR,WRLSU,WRLSW,PEBO,PEBK,PFIS,PBCT,PBCTP,PUB,PEP,PRCP,PRFT,PFMT,PERI,PESI,PPIH,PTX,PERY,PGLC,PETQ,PETS,PFSW,PGTI,PHII,PHIIK,PAHC,PLAB,PICO,PLLL,PIRS,PPC,PME,PNK,PNFP,PPSI,PXLW,EAGL,EAGLU,EAGLW,PLYA,PLXS,PLUG,PLBC,PS,PSTI,PLXP,PBSK,PNTR,PCOM,POLA,COOL,POOL,POPE,BPOP,BPOPM,BPOPN,PBIB,PTLA,PBPB,PCH,POWL,POWI,PRAA,PRAH,PRAN,PRPO,AIPT,PFBC,PLPC,PFBI,PINC,LENS,PSDO,PRGX,PSMT,PNRG,PRMW,PRIM,PVAL,PFG,BTEC,PXUS,GENY,PSET,PY,PMOM,USMC,PSC,PDEX,IPDN,PFIE,PGNX,PRGS,PFPT,PRPH,PRQR,EQRR,BIB,UBIO,TQQQ,ZBIO,SQQQ,BIS,PSEC,PTGX,PRTO,PTI,PRTA,PVBC,PROV,PBIP,PMD,PTC,PTCT,PULM,PLSE,PBYI,PACQ,PACQU,PACQW,PCYO,PRPL,PRPLW,PXS,QADA,QADB,QCRH,QGEN,QIWI,QRVO,QCOM,QSII,QBAK,QLYS,QTNA,QTRX,QTRH,QRHC,QUIK,QDEL,QNST,QUMU,QTNT,QRTEA,QRTEB,RRD,RCM,RARX,RADA,RDCM,RSYS,RDUS,RDNT,RDWR,METC,RMBS,RAND,GOLD,RNDB,RPD,RAVE,RAVN,RBB,ROLL,RICK,RCMT,RDI,RDIB,RGSE,BLCN,RNWK,RP,RETA,RCON,REPH,RRGB,RRR,RDVT,RDFN,RDHL,REGN,RGNX,RGLS,REIS,RBNC,RELV,MARK,RNST,REGI,ABAC,RCII,RGEN,RBCAA,FRBK,REFR,RSLS,RESN,RECN,HAIR,TORC,ROIC,RETO,RTRX,RVNC,RVEN,RVLT,RWLK,RFIL,RGCO,RYTM,RBBN,RIBT,RIBTW,RELL,RIGL,RNET,RMNI,RIOT,REDU,RTTR,RVSB,RLJE,RMGN,RCKT,RMTI,RCKY,RMCF,ROKU,ROSE,ROSEU,ROSEW,ROST,RGLD,RPXC,RTIX,RBCN,RMBL,RUSHA,RUSHB,RUTH,RXII,RXIIW,RYAAY,STBA,SANW,SCAC,SCACU,SCACW,SBRA,SABR,SAEX,SAFT,SAGE,SAIA,SALM,SAL,SAFM,SASR,SGMO,SANM,GCVRZ,SPNS,SRPT,SVRA,SBFG,SBFGP,SBBX,SBAC,SCSC,SMIT,SCHN,SRRK,SCHL,SGMS,SCPH,SCYX,SEAC,SBCF,STX,SHIP,SHIPW,SHLD,SHLDW,SHOS,SPNE,SGEN,EYES,EYESW,SECO,SCWX,SNFCA,SEIC,SLCT,SIR,SELB,SIGI,SLS,LEDS,SMTC,SENEA,SENEB,SNES,SNH,SNHNI,SNHNL,AIHS,SNMX,SRTS,SRTSW,STNL,STNLU,STNLW,SQBG,MCRB,SREV,SFBS,SESN,SSC,SVBI,SGBX,SGOC,SEII,SMED,SHSP,SHEN,PIXY,SHLO,TYHT,SHPG,SCVL,SHBI,SSTI,SFLY,SIFI,SIEB,SNNA,SIEN,BSRR,SRRA,SWIR,SIFY,SIGA,SIGM,SGLB,SGLBW,SGMA,SBNY,SBNYW,SLGN,SILC,SLAB,SIMO,SAMG,SSNT,SFNC,SLP,SINA,SBGI,SINO,SVA,SIRI,SITO,SKYS,SKYW,SWKS,SNBR,SLM,SLMBP,SGH,SND,SMBK,SMSI,SMTX,SRAX,SCKT,SODA,SOHU,SLRC,SUNS,SEDG,SLNO,SLNOW,SLGL,SLDB,SNGX,SNGXW,SONC,SOFO,SNOA,SNOAW,SPHS,SORL,SRNE,SOHO,SOHOB,SOHOO,SOHOK,SFBC,SSB,SFST,SMBC,SONA,SBSI,SP,SGRP,SPKE,SPKEP,ONCE,SPAR,SPTN,DWFI,SPPI,SPRO,ANY,SPEX,SPI,SAVE,STXB,SPLK,SPOK,SPWH,SBPH,FUND,SFM,SPSC,SSNC,SSLJ,SSRM,STAA,STAF,STMP,STND,SBLK,SBLKZ,SBUX,STFC,STBZ,STDY,GASS,STCN,STLD,SMRT,STLR,STLRU,STLRW,SBOT,STML,SRCL,SRCLP,SBT,STRL,SHOO,SSFN,SFIX,SYBT,BANX,SSKN,SSYS,HNDL,STRT,STRS,STRA,STRM,SBBP,SUMR,SMMF,SSBI,SMMT,SNHY,SNDE,SNSS,STKL,SPWR,RUN,SUNW,SMCI,SPCB,SCON,SGC,SUPN,SPRT,SURF,SGRY,SRDX,SIVB,SYKE,SYMC,SYNC,SYNL,SYNA,SNDX,SYNH,SGYP,SYBX,SNPS,SYNT,SES,SYPR,SYRS,TROW,TTOO,TRHC,TCMD,TAIT,TTWO,TLND,TNDM,TLF,TANH,TPIV,TEDU,TATT,TAYD,CGBD,TCPC,AMTD,PETZ,TECD,TCCO,TTGT,TGLS,TGEN,TNAV,TLGT,TELL,TENX,TERP,TBNK,TSRO,TSLA,TESS,TTEK,TTPH,TCBI,TCBIL,TCBIP,TCBIW,TXN,TXRH,TFSL,TGTX,ANDE,TBBK,BPRN,CG,TCGP,CAKE,CHEF,TCFC,DSGX,DXYN,ENSG,XONE,FINL,FBMS,FLIC,GT,HABT,HCKT,HAIN,CUBA,INTG,JYNT,KEYW,KHC,OLD,MSG,MDCO,MEET,MIK,MIDD,NAVG,SLIM,STKS,ORG,PRSC,RMR,SMPL,SMPLW,TSG,TTD,ULTI,YORW,NCTY,TXMD,TRPX,TBPH,TST,TCRD,TIBR,TIBRU,TIBRW,TIG,TTS,TSBK,TNTR,TIPT,TITN,TTNP,TVTY,TIVO,TMUS,TMSR,TOCA,TNXP,TISA,TOPS,TRCH,TRMD,TSEM,CLUB,TOWN,TPIC,TCON,TSCO,TWMC,TACT,TRNS,TGA,TA,TANNI,TANNL,TANNZ,TZOO,TRMT,TRVN,TCBK,TDACU,TRIL,TRS,TRMB,TRIB,TMCX,TMCXU,TMCXW,TRIP,TSC,TSCAP,TBK,TRVG,TRNC,TROV,TRUE,THST,TRUP,TRST,TRMK,TSRI,TTEC,TTMI,TCX,TUES,TOUR,HEAR,FOX,FOXA,TWIN,TRCB,TYME,USCR,PRTS,USEG,GROW,USAU,UBNT,UFPT,ULTA,UCTT,UPL,RARE,ULBI,UMBF,UMPQ,UNAM,UBSH,UNB,QURE,UBCP,UBOH,UBSI,UCBA,UCBI,UCFC,UBNK,UFCS,UIHC,UNFI,UBFO,USLM,UTHR,UG,UNIT,UNTY,UBX,OLED,UEIC,UFPI,ULH,USAP,UVSP,UMRX,UPLD,UONE,UONEK,URBN,URGN,ECOL,USAT,USATP,USAK,UTMD,UTSI,VLRX,VALX,VALU,VNDA,BBH,GNRX,PPH,VWOB,VNQI,VCIT,VGIT,VIGI,VYMI,VCLT,VGLT,VMBS,VONE,VONG,VONV,VTWO,VTWG,VTWV,VTHR,VCSH,VTIP,VGSH,VTC,BNDX,VXUS,VEAC,VEACU,VEACW,VREX,VRNS,VBLT,VXRT,VBIV,VTIQU,VECO,VEON,VRA,VCYT,VSTM,VCEL,VRNT,VRSN,VRSK,VBTX,VERI,VRML,VRNA,VSAR,VTNR,VRTX,VERU,VIA,VIAB,VSAT,VIAV,VICL,VICR,VCTR,CIZ,VSDA,CEY,CEZ,CID,CIL,CFO,CFA,CSF,CDC,CDL,VSMV,CSB,CSA,VRAY,VKTX,VKTXW,VBFC,VLGEA,VNOM,VIRC,VTSI,VIRT,VRTS,VRTSP,BBC,BBP,VRTU,VTGN,VC,VTL,VIVE,VVPR,VVUS,VOD,VOXX,VYGR,VSEC,VTVT,VUZI,WBA,WAFD,WAFDW,WASH,WSBF,WVE,WSTG,WCFB,WDFC,WEB,WB,WEBK,WEN,WERN,WSBC,WTBA,WABC,WSTL,WDC,WNEB,WPRT,WWR,WEYS,WHLR,WHLRD,WHLRP,WHLRW,WHF,WHFBL,WHLM,WVVI,WVVIP,WLDN,WLFC,WLTW,WSC,WSCWW,WIN,WING,WINA,WINS,WTFC,WTFCM,WTFCW,AGZD,AGND,CXSE,EMCG,EMCB,DGRE,DXGE,HYZD,WETF,DXJS,GULF,HYND,DGRW,DGRS,WIX,WMIH,WWD,WDAY,WKHS,WRLD,WMGI,WMGIZ,WSFS,WSCI,WVFC,WYNN,XBIT,XELB,XEL,XCRA,XNCR,XBIO,XENE,XGTI,XGTIW,XLNX,XOMA,XPER,XPLR,XSPA,XSPL,XTLB,XNET,YNDX,YRIV,YTRA,YTEN,YIN,YOGA,YGYI,YRCW,YECO,YY,ZFGN,ZAGG,ZLAB,ZEAL,ZBRA,Z,ZG,ZN,ZNWAA,ZION,ZIONW,ZIONZ,ZIOP,ZIXI,ZKIN,ZGNX,ZSAN,ZS,ZUMZ,ZYNE,ZNGA";
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
            for (int i = 0; i < size; i++) {
                subTickerArr[i] = tickerArr[i];
            }
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
