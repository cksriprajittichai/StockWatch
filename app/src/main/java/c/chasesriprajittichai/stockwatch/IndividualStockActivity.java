package c.chasesriprajittichai.stockwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.robinhood.spark.SparkView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import c.chasesriprajittichai.stockwatch.listeners.DownloadIndividualStockTaskListener;
import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;
import c.chasesriprajittichai.stockwatch.stocks.AfterHoursStock;
import c.chasesriprajittichai.stockwatch.stocks.BasicStock;
import c.chasesriprajittichai.stockwatch.stocks.PremarketStock;

import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.PREMARKET;
import static java.lang.Double.parseDouble;
import static org.apache.commons.lang3.StringUtils.substringBetween;


public class IndividualStockActivity extends AppCompatActivity implements DownloadIndividualStockTaskListener {

    private static class DownloadStockDataTask extends AsyncTask<Void, Integer, AdvancedStock> {

        private String mticker;
        private WeakReference<DownloadIndividualStockTaskListener> mcompletionListener;

        private DownloadStockDataTask(String ticker, DownloadIndividualStockTaskListener completionListener) {
            mticker = ticker.toUpperCase(Locale.US);
            mcompletionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected AdvancedStock doInBackground(Void... params) {
            AdvancedStock ret = null;

            try {
                /* Get graph data. */
                Document multiDoc = Jsoup.connect("https://www.marketwatch.com/investing/multi?tickers=" + mticker).get();
                Element quoteRoot = multiDoc.selectFirst("div[class~=section activeQuote bgQuote (down|up)?]");
                Element intradayChart = quoteRoot.selectFirst("script[type=text/javascript]");
                String jsStr = substringBetween(intradayChart.toString(), "Trades\":[", "]");
                // jsStr is in CSV format. Values in jsStr could be "null". Values do not contain ','.
                // If null values are found, replace them with the last non-null value.
                String[] chart_priceStrs = jsStr.split(",");
                ArrayList<Double> chart_prices = new ArrayList<>();

                // Init previous price as first non-null value
                double chart_prevPrice = -1;
                boolean chart_priceFound = false;
                for (String s : chart_priceStrs) {
                    if (!s.equals("null")) {
                        chart_prevPrice = parseDouble(s);
                        chart_priceFound = true;
                        break;
                    }
                }

                if (chart_priceFound) {
                    chart_prices.ensureCapacity(chart_priceStrs.length);
                    for (int i = 0; i < chart_priceStrs.length; i++) {
                        if (!chart_priceStrs[i].equals("null")) {
                            chart_prices.add(i, parseDouble(chart_priceStrs[i]));
                            chart_prevPrice = chart_prices.get(i); // Update prevPrice
                        } else {
                            chart_prices.add(i, chart_prevPrice);
                        }
                    }
                }


                /* Get all other data. */
                Document singleDoc = Jsoup.connect("https://www.marketwatch.com/investing/stock/" + mticker).get();

                Element nameElmnt = singleDoc.selectFirst("body div[data-symbol=" + mticker + "] div[class=row] > h1[class=company__name]");
                String name = nameElmnt.text();

                Element intraday = singleDoc.selectFirst("body div[class=element element--intraday]");
                Element intradayData = intraday.selectFirst("div[class=intraday__data]");

                Element icon = intraday.selectFirst("small[class~=intraday__status status--(before|open|after|closed)] > i[class^=icon]");
                String stateStr = icon.nextSibling().toString();
                BasicStock.State state;
                switch (stateStr.toLowerCase(Locale.US)) {
                    case "before the bell": // Multiple stock page uses this
                    case "premarket": // Individual stock page uses this
                        state = PREMARKET;
                        break;
                    case "open":
                        state = OPEN;
                        break;
                    case "after hours":
                        state = AFTER_HOURS;
                        break;
                    case "market closed": // Multiple stock view site uses this
                    case "closed":
                        state = CLOSED;
                        break;
                    default:
                        state = OPEN; /** Create error case (error state). */
                        break;
                }

                Element priceElmnt, changePointElmnt, changePercentElmnt, close_intradayDataElmnt,
                        close_priceElmnt, close_changePointElmnt, close_changePercentElmnt;
                Elements tableCells;
                double price, changePoint, changePercent, close_price, close_changePoint, close_changePercent;
                /* Do something different for each mstate. */
                switch (state) {
                    case PREMARKET: {
                        priceElmnt = intradayData.selectFirst("h3[class=intraday__price] > bg-quote[class^=value]");
                        changePointElmnt = intradayData.selectFirst("span[class=change--point--q] > bg-quote[field=change]");
                        changePercentElmnt = intradayData.selectFirst("span[class=change--percent--q] > bg-quote[field=percentchange]");

                        close_intradayDataElmnt = intraday.selectFirst("div[class=intraday__close]");
                        tableCells = close_intradayDataElmnt.select("tr[class=table__row] > td[class^=table__cell]");
                        close_priceElmnt = tableCells.get(0);
                        close_changePointElmnt = tableCells.get(1);
                        close_changePercentElmnt = tableCells.get(2);

                        // Remove ',' or '%' that could be in strings
                        price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                        changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                        changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                        close_price = parseDouble(close_priceElmnt.text().replaceAll("[^0-9.]+", ""));
                        close_changePoint = parseDouble(close_changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                        close_changePercent = parseDouble(close_changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));

                        ret = new PremarketStock(state, mticker, name, price, changePoint, changePercent,
                                close_price, close_changePoint, close_changePercent, chart_prices);
                        break;
                    }
                    case OPEN: {
                        priceElmnt = intradayData.selectFirst("h3[class=intraday__price] > bg-quote[class^=value]");
                        changePointElmnt = intradayData.selectFirst("bg-quote[class^=intraday__change] > span[class=change--point--q] > bg-quote[field=change]");
                        changePercentElmnt = intradayData.selectFirst("bg-quote[class^=intraday__change] > span[class=change--percent--q] > bg-quote[field=percentchange]");

                        // Remove ',' or '%' that could be in strings
                        price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                        changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                        changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));

                        ret = new AdvancedStock(state, mticker, name, price, changePoint, changePercent, chart_prices);
                        break;
                    }
                    case AFTER_HOURS: {
                        priceElmnt = intradayData.selectFirst("h3[class=intraday__price] > bg-quote[class^=value]");
                        changePointElmnt = intradayData.selectFirst("span[class=change--point--q] > bg-quote[field=change]");
                        changePercentElmnt = intradayData.selectFirst("span[class=change--percent--q] > bg-quote[field=percentchange]");

                        close_intradayDataElmnt = intraday.selectFirst("div[class=intraday__close]");
                        tableCells = close_intradayDataElmnt.select("tr[class=table__row] > td[class^=table__cell]");
                        close_priceElmnt = tableCells.get(0);
                        close_changePointElmnt = tableCells.get(1);
                        close_changePercentElmnt = tableCells.get(2);

                        // Remove ',' or '%' that could be in strings
                        price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                        changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                        changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                        close_price = parseDouble(close_priceElmnt.text().replaceAll("[^0-9.]+", ""));
                        close_changePoint = parseDouble(close_changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                        close_changePercent = parseDouble(close_changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));

                        ret = new AfterHoursStock(state, mticker, name, price, changePoint, changePercent,
                                close_price, close_changePoint, close_changePercent, chart_prices);
                        break;
                    }
                    case CLOSED: {
                        priceElmnt = intradayData.selectFirst("h3[class=intraday__price] > span[class=value]");
                        changePointElmnt = intradayData.selectFirst("bg-quote[class^=intraday__change] > span[class=change--point--q]");
                        changePercentElmnt = intradayData.selectFirst("bg-quote[class^=intraday__change] > span[class=change--percent--q]");

                        // Remove ',' or '%' that could be in strings
                        price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                        changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                        changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));

                        ret = new AdvancedStock(state, mticker, name, price, changePoint, changePercent, chart_prices);
                        break;
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            return ret;
        }

        @Override
        protected void onPostExecute(AdvancedStock stock) {
            mcompletionListener.get().onDownloadIndividualStockTaskCompleted(stock);
        }
    }

    private String mticker; // Needed to create mstock
    private AdvancedStock mstock;
    private boolean mwasInFavoritesInitially;
    private boolean misInFavorites;
    private TextView mscrubInfoTextView;
    private TextView mstateTextView;
    private TextView mpriceTextView;
    private TextView mchangePointTextView;
    private TextView mchangePercentTextView;
    private TextView mclose_priceTextView;
    private TextView mclose_changePointTextView;
    private TextView mclose_changePercentTextView;
    private SparkView msparkView;
    private SparkViewAdapter msparkViewAdapter;
    private SharedPreferences mpreferences;

    @Override
    public void onDownloadIndividualStockTaskCompleted(AdvancedStock stock) {
        mstock = stock;

        if (getTitle().equals("")) {
            setTitle(mstock.getName());
        }

        msparkViewAdapter.setyData(mstock.getyData());
        msparkViewAdapter.notifyDataSetChanged();

        mscrubInfoTextView.setText(getString(R.string.double2dec, mstock.getPrice())); // Init text view

        msparkView.setScrubListener((Object valueObj) -> {
            if (valueObj == null) {
                mscrubInfoTextView.setText(getString(R.string.double2dec, mstock.getPrice()));
                int color_deactivated = getResources().getColor(R.color.colorAccentTransparent, getTheme());
                mscrubInfoTextView.setTextColor(color_deactivated);
            } else {
                mscrubInfoTextView.setText(getString(R.string.double2dec, (double) valueObj));
                int color_activated = getResources().getColor(R.color.colorAccent, getTheme());
                mscrubInfoTextView.setTextColor(color_activated);
            }
        });

        mstateTextView.setText(getString(R.string.string_colon_string, "State", mstock.getState().toString()));
        mpriceTextView.setText(getString(R.string.string_colon_double2dec, "Price", mstock.getPrice()));
        mchangePointTextView.setText(getString(R.string.string_colon_double2dec, "Point Change", mstock.getChangePoint()));
        mchangePercentTextView.setText(getString(R.string.string_colon_double2dec_percent, "Percent Change", mstock.getChangePercent()));
        if (mstock instanceof AfterHoursStock) {
            AfterHoursStock ahStock = (AfterHoursStock) mstock;
            mclose_priceTextView.setText(getString(R.string.string_colon_double2dec, "Price at Close", ahStock.getClose_price()));
            mclose_changePointTextView.setText(getString(R.string.string_colon_double2dec, "Point Change at Close", ahStock.getClose_changePoint()));
            mclose_changePercentTextView.setText(getString(R.string.string_colon_double2dec_percent, "Percent Change at Close", ahStock.getClose_changePercent()));
        } else if (mstock instanceof PremarketStock) {
            PremarketStock ahStock = (PremarketStock) mstock;
            mclose_priceTextView.setText(getString(R.string.string_colon_double2dec, "Price at Close", ahStock.getClose_price()));
            mclose_changePointTextView.setText(getString(R.string.string_colon_double2dec, "Point Change at Close", ahStock.getClose_changePoint()));
            mclose_changePercentTextView.setText(getString(R.string.string_colon_double2dec_percent, "Percent Change at Close", ahStock.getClose_changePercent()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);
        setTitle(""); // Show empty title now, company name will be shown (in onPostExecute())
        mpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mticker = getIntent().getStringExtra("Ticker");
        misInFavorites = getIntent().getBooleanExtra("Is in favorites", false);
        mwasInFavoritesInitially = misInFavorites;

        // Start task ASAP
        DownloadStockDataTask task = new DownloadStockDataTask(mticker, this);
        task.execute();

        mscrubInfoTextView = findViewById(R.id.textView_scrub);
        mstateTextView = findViewById(R.id.test_state);
        mpriceTextView = findViewById(R.id.test_price);
        mchangePointTextView = findViewById(R.id.test_changePoint);
        mchangePercentTextView = findViewById(R.id.test_changePercent);
        mclose_priceTextView = findViewById(R.id.test_close_price);
        mclose_changePointTextView = findViewById(R.id.test_close_changePoint);
        mclose_changePercentTextView = findViewById(R.id.test_close_changePercent);

        msparkViewAdapter = new SparkViewAdapter(new ArrayList<>()); // Init as empty
        msparkView = findViewById(R.id.sparkView);
        msparkView.setAdapter(msparkViewAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (misInFavorites != mwasInFavoritesInitially) {
            // If the star status (favorite status) has changed
            if (mwasInFavoritesInitially) {
                removeStockFromPreferences();
            } else {
                addStockToPreferences();
            }

            /* The activity has paused. Update the favorites status.
             * This is vital because the condition to be able to edit Tickers CSV and Data CSV are
             * whether or not the favorites status has changed. */
            mwasInFavoritesInitially = misInFavorites;
        }
    }

    /* Back button is the only way to get back to HomeActivity. */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stock_activity, menu);
        MenuItem starItem = menu.findItem(R.id.starMenuItem);
        starItem.setIcon(misInFavorites ? R.drawable.star_on : R.drawable.star_off);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.starMenuItem:
                misInFavorites = !misInFavorites; // Toggle
                item.setIcon(misInFavorites ? R.drawable.star_on : R.drawable.star_off);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adds mstock to mpreferences; adds mstock's ticker to Tickers CSV and and adds mstock's data
     * to Data CSV. mstock is added to the front of each preference string, meaning that mstock
     * is inserted at the top of the list of stocks. This function does not check if mstock is
     * already in mpreferences before adding mstock.
     */
    private void addStockToPreferences() {
        final String tickersCSV = mpreferences.getString("Tickers CSV", "");
        final String dataCSV = mpreferences.getString("Data CSV", "");
        String dataStr;

        if (!tickersCSV.isEmpty()) {
            mpreferences.edit().putString("Tickers CSV", mticker + ',' + tickersCSV).apply();
            dataStr = mstock.getState().toString() + ',' + mstock.getPrice() + ',' +
                    mstock.getChangePoint() + ',' + mstock.getChangePercent() + ',';
            mpreferences.edit().putString("Data CSV", dataStr + dataCSV).apply();
        } else {
            mpreferences.edit().putString("Tickers CSV", mticker).apply();
            dataStr = mstock.getState().toString() + ',' + mstock.getPrice() + ',' +
                    mstock.getChangePoint() + ',' + mstock.getChangePercent();
            mpreferences.edit().putString("Data CSV", dataStr).apply();
        }
    }

    /**
     * Removes mstock from mpreferences; removes mstock's ticker from Tickers CSV and removes
     * mStock's data from Data CSV.
     */
    private void removeStockFromPreferences() {
        final String tickersCSV = mpreferences.getString("Tickers CSV", "");
        final String[] tickerArr = tickersCSV.split(","); // "".split(",") returns {""}

        if (!tickerArr[0].isEmpty()) {
            final ArrayList<String> tickerList = new ArrayList<>(Arrays.asList(tickerArr));

            int tickerNdx = tickerList.indexOf(mstock.getTicker());
            if (tickerNdx != -1) {
                /* Delete mstock's mticker. */
                tickerList.remove(tickerNdx);
                mpreferences.edit().putString("Tickers CSV", String.join(",", tickerList)).apply();

                /* Delete mstock's data. */
                String dataCSV = mpreferences.getString("Data CSV", "");
                final ArrayList<String> dataList = new ArrayList<>(Arrays.asList(dataCSV.split(",")));

                // 4 data elements per 1 mticker. DataNdx is the index of the first element to delete.
                int dataNdx = tickerNdx * 4;
                for (int deleteCount = 1; deleteCount <= 4; deleteCount++) { // Delete 4 data elements
                    dataList.remove(dataNdx);
                }
                mpreferences.edit().putString("Data CSV", String.join(",", dataList)).apply();
            }
        }
    }
}
