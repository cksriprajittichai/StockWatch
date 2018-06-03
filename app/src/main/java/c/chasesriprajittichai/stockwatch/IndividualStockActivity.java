package c.chasesriprajittichai.stockwatch;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import java.util.Locale;

import c.chasesriprajittichai.stockwatch.AsyncTaskListeners.DownloadIndividualStockTask;

import static c.chasesriprajittichai.stockwatch.BasicStock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.BasicStock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.BasicStock.State.OPEN;
import static java.lang.Double.parseDouble;
import static org.apache.commons.lang3.StringUtils.substringBetween;


public class IndividualStockActivity extends AppCompatActivity implements DownloadIndividualStockTask {

    private static class DownloadStockDataTask extends AsyncTask<Void, Integer, Integer> {

        private String ticker;
        private WeakReference<AdvancedStock> stock;
        private WeakReference<DownloadIndividualStockTask> completionListener;

        private DownloadStockDataTask(String ticker, AdvancedStock stock, DownloadIndividualStockTask completionListener) {
            this.ticker = ticker.toUpperCase(Locale.US);
            this.stock = new WeakReference<>(stock);
            this.completionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                /* Get graph data. */
                Document multiDoc = Jsoup.connect("https://www.marketwatch.com/investing/multi?tickers=" + ticker).get();
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
                Document singleDoc = Jsoup.connect("https://www.marketwatch.com/investing/stock/" + ticker).get();

                Element nameElmnt = singleDoc.selectFirst("body div[data-symbol=" + ticker + "] div[class=row] > h1[class=company__name]");
                String name = nameElmnt.text();

                Element intraday = singleDoc.selectFirst("body div[class=element element--intraday]");
                Element intradayData = intraday.selectFirst("div[class=intraday__data]");

                Element icon = intraday.selectFirst(
                        "small[class~=intraday__status status--(open|after|closed)] > i[class^=icon]");
                String stateStr = icon.nextSibling().toString();
                BasicStock.State state;
                switch (stateStr.toLowerCase(Locale.US)) {
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
                /* Do something different for each state. */
                switch (state) {
                    case OPEN: {
                        priceElmnt = intradayData.selectFirst("h3[class=intraday__price] > bg-quote[class^=value]");
                        changePointElmnt = intradayData.selectFirst("bg-quote[class^=intraday__change] > span[class=change--point--q] > bg-quote[field=change]");
                        changePercentElmnt = intradayData.selectFirst("bg-quote[class^=intraday__change] > span[class=change--percent--q] > bg-quote[field=percentchange]");

                        // Remove ',' or '%' that could be in strings
                        price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                        changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                        changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));

                        stock = new WeakReference<>(
                                new AdvancedStock(state, ticker, name, price, changePoint,
                                        changePercent, chart_prices));
                        break;
                    }
                    case AFTER_HOURS: {
                        priceElmnt = intradayData.selectFirst("h3[class=intraday__price] > bg-quote[class^=value]");
                        changePointElmnt = intradayData.selectFirst("span[class=change--point--q] > bg-quote[field=change]");
                        changePercentElmnt = intradayData.selectFirst("span[class=change--percent--q] > bg-quote[field=percentchange]");

                        close_intradayDataElmnt = intraday.selectFirst("div[class=intraday__close]");
                        tableCells = close_intradayDataElmnt.select("tr[class=table__row] > " +
                                "td[class^=table__cell]");
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

                        stock = new WeakReference<>(
                                new AfterHoursStock(state, ticker, name, price, changePoint, changePercent,
                                        close_price, close_changePoint, close_changePercent, chart_prices));
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

                        stock = new WeakReference<>(
                                new AdvancedStock(state, ticker, name, price, changePoint,
                                        changePercent, chart_prices));
                        break;
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer status) {
            completionListener.get().onDownloadIndividualStockTaskCompleted(stock.get());
        }
    }

    private String ticker; // Needed to create stock
    private AdvancedStock stock;
    private boolean wasInFavoritesInitially;
    private boolean isInFavorites;
    private TextView scrubInfoTextView;
    private TextView priceTextView;
    private TextView changePointTextView;
    private TextView changePercentTextView;
    private TextView close_priceTextView;
    private TextView close_changePointTextView;
    private TextView close_changePercentTextView;
    private SparkView sparkView;
    private SparkViewAdapter sparkViewAdapter;
    private SharedPreferences preferences;

    @Override
    public void onDownloadIndividualStockTaskCompleted(AdvancedStock stock) {
        this.stock = stock;

        if (getTitle().equals("")) {
            setTitle(stock.getName());
        }

        sparkViewAdapter.setyData(stock.getyData());
        sparkViewAdapter.notifyDataSetChanged();

        scrubInfoTextView.setText(getString(R.string.double2dec, stock.getPrice())); // Init text view

        sparkView.setScrubListener((Object valueObj) -> {
            if (valueObj == null) {
                scrubInfoTextView.setText(getString(R.string.double2dec, stock.getPrice()));
                int color_deactivated = getResources().getColor(R.color.colorAccentTransparent, getTheme());
                scrubInfoTextView.setTextColor(color_deactivated);
            } else {
                scrubInfoTextView.setText(getString(R.string.double2dec, (double) valueObj));
                int color_activated = getResources().getColor(R.color.colorAccent, getTheme());
                scrubInfoTextView.setTextColor(color_activated);
            }
        });

        priceTextView.setText(getString(R.string.string_colon_double2dec, "Price", stock.getPrice()));
        changePointTextView.setText(getString(R.string.string_colon_double2dec, "Point Change", stock.getChangePoint()));
        changePercentTextView.setText(getString(R.string.string_colon_double2dec_percent, "Percent Change", stock.getChangePercent()));
        if (stock instanceof AfterHoursStock) {
            AfterHoursStock ahStock = (AfterHoursStock) stock;
            close_priceTextView.setText(getString(R.string.string_colon_double2dec, "Price at Close", ahStock.getClose_price()));
            close_changePointTextView.setText(getString(R.string.string_colon_double2dec, "Point Change at Close", ahStock.getClose_changePoint()));
            close_changePercentTextView.setText(getString(R.string.string_colon_double2dec_percent, "Percent Change at Close", ahStock.getClose_changePercent()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);
        setTitle(""); // Show empty title now, company name will be shown (in onPostExecute())
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ticker = getIntent().getStringExtra("Ticker");
        isInFavorites = getIntent().getBooleanExtra("Is in favorites", false);
        wasInFavoritesInitially = isInFavorites;

        DownloadStockDataTask task = new DownloadStockDataTask(ticker, stock, this);
        task.execute();

        scrubInfoTextView = findViewById(R.id.textView_scrub);
        priceTextView = findViewById(R.id.test_price);
        changePointTextView = findViewById(R.id.test_changePoint);
        changePercentTextView = findViewById(R.id.test_changePercent);
        close_priceTextView = findViewById(R.id.test_close_price);
        close_changePointTextView = findViewById(R.id.test_close_changePoint);
        close_changePercentTextView = findViewById(R.id.test_close_changePercent);

        sparkViewAdapter = new SparkViewAdapter(new ArrayList<>()); // Init as empty
        sparkView = findViewById(R.id.sparkView);
        sparkView.setAdapter(sparkViewAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isInFavorites != wasInFavoritesInitially) {
            // If the star status (favorite status) has changed
            if (wasInFavoritesInitially) {
                // Remove fullStock's ticker from Tickers CSV preference
                String[] tickers = preferences.getString("Tickers CSV", "").split(",");
                String[] data = preferences.getString("Data CSV", "").split(",");
                StringBuilder tickersCSV = new StringBuilder(tickers.length * 5); // Approximate
                StringBuilder dataCSV = new StringBuilder(data.length * 5); // Approximate
                for (int tickerNdx = 0, dataNdx = 0; tickerNdx < tickers.length; tickerNdx++, dataNdx += 4) {
                    if (!tickers[tickerNdx].equalsIgnoreCase(ticker)) {
                        tickersCSV.append(tickers[tickerNdx]);
                        tickersCSV.append(',');

                        dataCSV.append(data[dataNdx] + ',' + data[dataNdx + 1] + ',' +
                                data[dataNdx + 2] + ',' + data[dataNdx + 3] + ',');
                    }
                }

                if (!tickersCSV.toString().isEmpty()) {
                    tickersCSV.deleteCharAt(tickersCSV.length() - 1); // Delete extra comma
                    dataCSV.deleteCharAt(dataCSV.length() - 1); // Delete extra comma
                    preferences.edit().putString("Tickers CSV", tickersCSV.toString()).apply();
                    preferences.edit().putString("Data CSV", dataCSV.toString()).apply();
                } else {
                    // The only ticker in favorites has been removed
                    preferences.edit().putString("Tickers CSV", "").apply();
                    preferences.edit().putString("Data CSV", "").apply();
                }
            } else {
                // Add stock's ticker to Tickers CSV preference.
                // Add stock's data to Data CSV preference.
                // Insert stock at the front of the string (top of the list).
                String tickersCSV = preferences.getString("Tickers CSV", "");
                String dataCSV = preferences.getString("Data CSV", "");
                if (tickersCSV.isEmpty()) {
                    preferences.edit().putString("Tickers CSV", stock.getTicker()).apply();
                    preferences.edit().putString("Data CSV", stock.getState().toString() + ',' +
                            stock.getPrice() + ',' + stock.getChangePoint() + ',' +
                            stock.getChangePercent()).apply();
                } else {
                    preferences.edit().putString("Tickers CSV", stock.getTicker() + ',' + tickersCSV).apply();
                    preferences.edit().putString("Data CSV", stock.getState().toString() + ',' +
                            stock.getPrice() + ',' + stock.getChangePoint() + ',' +
                            stock.getChangePercent() + ',' + dataCSV).apply();
                }
            }

            /* The activity has paused. Update the favorites status.
             * This is vital because the condition to be able to edit Tickers CSV and Data CSV are
             * whether or not the favorites status has changed. */
            wasInFavoritesInitially = isInFavorites;
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

        if (isInFavorites) {
            menu.findItem(R.id.starMenuItem).setIcon(R.drawable.star_on);
        } else {
            menu.findItem(R.id.starMenuItem).setIcon(R.drawable.star_off);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.starMenuItem:
                isInFavorites = !isInFavorites; // Toggle
                if (isInFavorites) {
                    item.setIcon(R.drawable.star_on);
                } else {
                    item.setIcon(R.drawable.star_off);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
