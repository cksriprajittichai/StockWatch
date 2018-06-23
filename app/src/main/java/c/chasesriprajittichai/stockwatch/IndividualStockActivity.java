package c.chasesriprajittichai.stockwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.robinhood.spark.SparkView;
import com.wefika.horizontalpicker.HorizontalPicker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
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
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBetween;


public final class IndividualStockActivity extends AppCompatActivity implements
        DownloadIndividualStockTaskListener, HorizontalPicker.OnItemSelected {

    private static final class DownloadStockDataTask extends AsyncTask<Void, Integer, AdvancedStock> {

        private final String mticker;

        /* It is quite common that a stock's stat is missing. If this is the case, "n/a" replaces
         * the value that should be there, or in rarer cases, there is an empty string replacing
         * the value that should be there. */
        private final HashSet<String> missingStats = new HashSet<>();

        private final WeakReference<DownloadIndividualStockTaskListener> mcompletionListener;

        private DownloadStockDataTask(final String ticker, final DownloadIndividualStockTaskListener completionListener) {
            mticker = ticker;
            mcompletionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected AdvancedStock doInBackground(final Void... params) {
            final AdvancedStock ret;

            /* Get 1 day chart data. */
            final Document multiDoc;
            try {
                multiDoc = Jsoup.connect("https://www.marketwatch.com/investing/multi?tickers=" + mticker).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                final ArrayList<Double> empty = new ArrayList<>();
                return new AdvancedStock(OPEN, "", "", -1, -1,
                        -1, -1, -1, -1,
                        -1, -1, "", -1,
                        -1, -1, -1, "", "",
                        empty, empty, empty, empty, empty, empty);
            }

            /* Some stocks have no chart data. If this is the case, chart_prices will be an
             * empty array list. */
            final ArrayList<Double> chart_prices_1day = new ArrayList<>();

            final Element multiQuoteRoot = multiDoc.selectFirst("html > body > div#blanket > div[class*=multi] > div#maincontent > " +
                    "div[class^=block multiquote] > div[class^=quotedisplay] > div[class^=section activeQuote bgQuote]");
            final Element javascriptElmnt = multiQuoteRoot.selectFirst(":root > div.intradaychart > script[type=text/javascript]");

            /* If there is no chart data, javascriptElmnt element still exists in the HTML and
             * there is still some javascript code in javascriptElmnt.toString(). There is just no
             * chart data embedded in the code. This means that the call to substringBetween() on
             * javascriptElmnt.toString() will return null, because no substring between the
             * open and close parameters (substringBetween() parameters) exists. */
            final String javascriptStr = substringBetween(javascriptElmnt.toString(), "Trades\":[", "]");
            if (javascriptStr != null) {
                /* javascriptStr is in CSV format. Values in javascriptStr could be "null". Values
                 * do not contain ','. If null values are found, replace them with the last
                 * non-null value. */
                final String[] chart_priceStrs = javascriptStr.split(",");

                // Init chart_prevPrice as first non-null value
                double chart_prevPrice = -1;
                boolean chart_priceFound = false;
                for (final String s : chart_priceStrs) {
                    if (!s.equals("null")) {
                        chart_prevPrice = parseDouble(s);
                        chart_priceFound = true;
                        break;
                    }
                }
                // Fill chart_prices
                if (chart_priceFound) {
                    chart_prices_1day.ensureCapacity(chart_priceStrs.length);
                    for (int i = 0; i < chart_priceStrs.length; i++) {
                        if (!chart_priceStrs[i].equals("null")) {
                            chart_prices_1day.add(i, parseDouble(chart_priceStrs[i]));
                            chart_prevPrice = chart_prices_1day.get(i); // Update prevPrice
                        } else {
                            chart_prices_1day.add(i, chart_prevPrice);
                        }
                    }
                }
            }


            final Document individualDoc;
            try {
                individualDoc = Jsoup.connect("https://www.marketwatch.com/investing/stock/" + mticker).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                final ArrayList<Double> empty = new ArrayList<>();
                return new AdvancedStock(OPEN, "", "", -1, -1,
                        -1, -1, -1, -1,
                        -1, -1, "", -1,
                        -1, -1, -1, "", "",
                        empty, empty, empty, empty, empty, empty);
            }

            /* Get chart data for periods greater than one day from Wall Street Journal. */
            // Certain values from Market Watch's individual-stock-site are needed in the WSJ url
            final String wsj_instrumentType = individualDoc.selectFirst(":root > head > meta[name=instrumentType]").attr("content");
            final String wsj_exchange = individualDoc.selectFirst(":root > head > meta[name=exchangeIso]").attr("content");
            final String wsj_exchangeCountry = individualDoc.selectFirst(":root > head > meta[name=exchangeCountry]").attr("content");

            final LocalDate today = LocalDate.now();
            /* Deduct extra because it doesn't hurt at all, and ensures that the URL we create
             * doesn't incorrectly believe that there isn't enough data for the five year chart. */
            final LocalDate fiveYearsAgo = today.minusYears(5).minusMonths(1);
            final int period_5years = (int) ChronoUnit.DAYS.between(fiveYearsAgo, today);

            // Pad zeros on the left if necessary
            final String wsj_todayDateStr = String.format(Locale.US, "%d/%d/%d", today.getMonthValue(),
                    today.getDayOfMonth(), today.getYear());
            final String wsj_5yearsAgoDateStr = String.format(Locale.US, "%d/%d/%d", fiveYearsAgo.getMonthValue(),
                    fiveYearsAgo.getDayOfMonth(), fiveYearsAgo.getYear());

            /* If the WSJ URL parameters (ie. start date) request data from dates that are prior to
             * a stock's existence, then the WSJ response delivers all historical data available for
             * the stock. Because five years is the largest chart period we are using, data for the
             * smaller periods can be grabbed from the five year WSJ response page. The actual
             * number of data points that exists for a stock can be determined by parsing the WSJ
             * response and counting the number of certain elements (ie. table rows). */
            final String wsj_url_5years = String.format(Locale.US,
                    "https://quotes.wsj.com/ajax/historicalprices/4/%s?MOD_VIEW=page&ticker=%s&country=%s" +
                            "&exchange=%s&instrumentType=%s&num_rows=%d&range_days=%d&startDate=%s&endDate=%s",
                    mticker, mticker, wsj_exchangeCountry, wsj_exchange, wsj_instrumentType,
                    period_5years, period_5years, wsj_5yearsAgoDateStr, wsj_todayDateStr);

            final ArrayList<Double> chartPrices_5years = new ArrayList<>();
            final ArrayList<Double> chartPrices_1year = new ArrayList<>();
            final ArrayList<Double> chartPrices_3months = new ArrayList<>();
            final ArrayList<Double> chartPrices_1month = new ArrayList<>();
            final ArrayList<Double> chartPrices_2weeks = new ArrayList<>();
            final List<ArrayList<Double>> chartPricesList = new ArrayList<>(Arrays.asList(
                    chartPrices_5years, chartPrices_1year, chartPrices_3months, chartPrices_1month, chartPrices_2weeks));

            Document fiveYearDoc = null;
            // Loop and fill chart prices for each chart period
            try {
                fiveYearDoc = Jsoup.connect(wsj_url_5years).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                /** Add to missing stats? */
            }

            if (fiveYearDoc != null) {
                int i, periodNdx;

                /* This is the number of data points needed for each period. The stock market is
                 * only open on weekdays, and there are 9 holidays that the stock market closes for.
                 * So the stock market is open for ~252 days a year. Use this value to approximate
                 * the number of data points that should be in each period. */
                final int[] PERIODS = {1260, 252, 63, 21, 10};
                /* Don't get too many data points for longer chart periods because it takes too long
                 * and is unnecessary. Take no more than 200 data points max in a period. */
                final int[] PERIOD_INCREMENTS = {8, 2, 1, 1, 1};

                /* Charts use the closing price of each day. The closing price is the 5th column
                 * in each table row. */
                final Elements priceElmnts = fiveYearDoc.select(":root > body > div > div#historical_data_table > div > table > tbody > tr > :eq(4)");
                final int NUM_DATA_POINTS = priceElmnts.size() <= 1260 ? priceElmnts.size() : 1260;
                final ArrayList<Double> allChartPrices = new ArrayList<>(NUM_DATA_POINTS);

                /* The most recent prices are at the top of the WSJ page (top of tableRows), and
                 * the oldest prices are at the bottom. Fill allChartPrices starting with the last
                 * price elements so that the oldest prices are the front of allPrices and the
                 * recent prices are at the end. */
                for (i = NUM_DATA_POINTS - 1; i >= 0; i--) {
                    allChartPrices.add(parseDouble(priceElmnts.get(i).text()));
                }

                /* Fill chartPrices for each period. If there is not enough data to represent a full
                 * period, then leave that period's chartPrice empty. */
                ArrayList<Double> curChartPrices;
                for (periodNdx = 0; periodNdx < chartPricesList.size(); periodNdx++) {
                    if (PERIODS[periodNdx] <= NUM_DATA_POINTS) {
                        // If there are enough data points to fill this period

                        curChartPrices = chartPricesList.get(periodNdx);
                        for (i = NUM_DATA_POINTS - PERIODS[periodNdx]; i < NUM_DATA_POINTS; i += PERIOD_INCREMENTS[periodNdx]) {
                            curChartPrices.add(allChartPrices.get(i));
                        }
                    }
                }
            }


            /* Get non-chart data. */
            final Element quoteRoot = individualDoc.selectFirst(":root > body[role=document] > div[data-symbol=" + mticker + "]");

            final Element regionFixed = quoteRoot.selectFirst(":root > div.content-region.region--fixed");

            final Element nameElmnt = regionFixed.selectFirst(":root > div > div.column.column--full.company div.row > h1.company__name");
            final String name = nameElmnt.text();

            final Element intraday = regionFixed.selectFirst(":root > div.template.template--aside > div > div.element.element--intraday");
            final Element intradayData = intraday.selectFirst(":root > div.intraday__data");

            final Element icon = intraday.selectFirst(":root > small[class~=intraday__status status--(before|open|after|closed)] > i[class^=icon]");
            final String stateStr = icon.nextSibling().toString();
            final BasicStock.State state;
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

            final Element priceElmnt, changePointElmnt, changePercentElmnt, close_intradayElmnt,
                    close_priceElmnt, close_changePointElmnt, close_changePercentElmnt;
            final Elements close_tableCells;
            final double price, changePoint, changePercent, close_price, close_changePoint, close_changePercent;
            // Parsing of certain data varies depending on the state of the stock.
            switch (state) {
                case PREMARKET: {
                    priceElmnt = intradayData.selectFirst(":root > h3.intraday__price > bg-quote[class^=value]");
                    changePointElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--point--q > bg-quote[field=change]");
                    changePercentElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--percent--q > bg-quote[field=percentchange]");

                    close_intradayElmnt = intraday.selectFirst(":root > div.intraday__close");
                    close_tableCells = close_intradayElmnt.select(":root > table > tbody > tr.table__row > td[class^=table__cell]");
                    close_priceElmnt = close_tableCells.get(0);
                    close_changePointElmnt = close_tableCells.get(1);
                    close_changePercentElmnt = close_tableCells.get(2);

                    // Remove ',' or '%' that could be in strings
                    price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                    close_price = parseDouble(close_priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    close_changePoint = parseDouble(close_changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    close_changePercent = parseDouble(close_changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                    break;
                }
                case OPEN: {
                    priceElmnt = intradayData.selectFirst(":root > h3.intraday__price > bg-quote[class^=value]");
                    changePointElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--point--q > bg-quote[field=change]");
                    changePercentElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--percent--q > bg-quote[field=percentchange]");

                    // Remove ',' or '%' that could be in strings
                    price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));

                    // Initialize unused data
                    close_price = 0;
                    close_changePoint = 0;
                    close_changePercent = 0;
                    break;
                }
                case AFTER_HOURS: {
                    priceElmnt = intradayData.selectFirst(":root > h3.intraday__price > bg-quote[class^=value]");
                    changePointElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--point--q > bg-quote[field=change]");
                    changePercentElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--percent--q > bg-quote[field=percentchange]");

                    close_intradayElmnt = intraday.selectFirst(":root > div.intraday__close");
                    close_tableCells = close_intradayElmnt.select(":root > table > tbody > tr.table__row > td");
                    close_priceElmnt = close_tableCells.get(0);
                    close_changePointElmnt = close_tableCells.get(1);
                    close_changePercentElmnt = close_tableCells.get(2);

                    // Remove ',' or '%' that could be in strings
                    price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                    close_price = parseDouble(close_priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    close_changePoint = parseDouble(close_changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    close_changePercent = parseDouble(close_changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                    break;
                }
                case CLOSED: {
                    priceElmnt = intradayData.selectFirst(":root > h3.intraday__price > span.value");
                    changePointElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--point--q");
                    changePercentElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--percent--q");

                    // Remove ',' or '%' that could be in strings
                    price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));

                    // Initialize unused data
                    close_price = 0;
                    close_changePoint = 0;
                    close_changePercent = 0;
                    break;
                }
                default: { /** Create error state. */
                    price = -1;
                    changePoint = -1;
                    changePercent = -1;
                    close_price = -1;
                    close_changePoint = -1;
                    close_changePercent = -1;
                }
            }

            final Element regionPrimary = quoteRoot.selectFirst(":root > div.content-region.region--primary");
            final Element keyDataElmnt = regionPrimary.selectFirst(":root > div.template.template--aside > div.column.column--full.left.clearfix > div.element.element--list > ul.list.list--kv.list--col50");
            final Elements keyDataItemElmnts = keyDataElmnt.select(":root > li");

            final Element openPriceElmnt = keyDataItemElmnts.get(0);
            final Element dayRangeElmnt = keyDataItemElmnts.get(1);
            final Element fiftyTwoWeekRangeElmnt = keyDataItemElmnts.get(2);
            final Element marketCapElmnt = keyDataItemElmnts.get(3);
            final Element betaElmnt = keyDataItemElmnts.get(6);
            final Element peRatioElmnt = keyDataItemElmnts.get(8);
            final Element epsElmnt = keyDataItemElmnts.get(9);
            final Element yieldElmnt = keyDataItemElmnts.get(10);
            final Element avgVolumeElmnt = keyDataItemElmnts.get(15);

            final double openPrice;
            // Remove ',' or '%' that could be in strings
            if (!openPriceElmnt.text().contains("n/a") && !openPriceElmnt.text().equalsIgnoreCase("Open")) {
                openPrice = parseDouble(openPriceElmnt.text().replaceAll("[^0-9.]+", ""));
            } else {
                openPrice = -1;
                missingStats.add("Price at Open");
            }
            final double dayRangeLow, dayRangeHigh;
            if (!dayRangeElmnt.text().contains("n/a") && !dayRangeElmnt.text().equalsIgnoreCase("Day Range")) {
                dayRangeLow = parseDouble(substringBefore(dayRangeElmnt.text(), "-").replaceAll("[^0-9.]+", ""));
                dayRangeHigh = parseDouble(substringAfter(dayRangeElmnt.text(), "-").replaceAll("[^0-9.]+", ""));
            } else {
                dayRangeLow = -1;
                dayRangeHigh = -1;
                missingStats.add("Day Range");
            }
            final double fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh;
            if (!fiftyTwoWeekRangeElmnt.text().contains("n/a") && !fiftyTwoWeekRangeElmnt.text().equalsIgnoreCase("52 Week Range")) {
                fiftyTwoWeekRangeLow = parseDouble(substringBetween(fiftyTwoWeekRangeElmnt.text(), "52 Week Range", "-").replaceAll("[^0-9.]+", ""));
                fiftyTwoWeekRangeHigh = parseDouble(substringAfter(fiftyTwoWeekRangeElmnt.text(), "-").replaceAll("[^0-9.]+", ""));
            } else {
                fiftyTwoWeekRangeLow = -1;
                fiftyTwoWeekRangeHigh = -1;
                missingStats.add("52 Week Range");
            }
            final String marketCap;
            if (!marketCapElmnt.text().contains("n/a") && !marketCapElmnt.text().equalsIgnoreCase("Market Cap")) {
                marketCap = substringAfter(marketCapElmnt.text(), "Market Cap").trim();
            } else {
                marketCap = "";
                missingStats.add("Market Cap");
            }
            final double beta;
            if (!betaElmnt.text().contains("n/a") && !betaElmnt.text().equalsIgnoreCase("Beta")) {
                beta = parseDouble(betaElmnt.text().replaceAll("[^0-9.]+", ""));
            } else {
                beta = -1;
                missingStats.add("Beta");
            }
            final double peRatio;
            if (!peRatioElmnt.text().contains("n/a") && !peRatioElmnt.text().equalsIgnoreCase("P/E Ratio")) {
                peRatio = parseDouble(peRatioElmnt.text().replaceAll("[^0-9.]+", ""));
            } else {
                peRatio = -1;
                missingStats.add("P/E Ratio");
            }
            final double eps;
            if (!epsElmnt.text().contains("n/a") && !epsElmnt.text().equalsIgnoreCase("EPS")) {
                eps = parseDouble(epsElmnt.text().replaceAll("[^0-9.]+", ""));
            } else {
                eps = -1;
                missingStats.add("EPS");
            }
            final double yield;
            if (!yieldElmnt.text().contains("n/a") && !yieldElmnt.text().equalsIgnoreCase("Yield")) {
                yield = parseDouble(yieldElmnt.text().replaceAll("[^0-9.]+", ""));
            } else {
                yield = -1;
                missingStats.add("Yield");
            }
            final String avgVolume;
            if (!avgVolumeElmnt.text().contains("n/a") && !avgVolumeElmnt.text().equalsIgnoreCase("Average Volume")) {
                avgVolume = substringAfter(avgVolumeElmnt.text(), "Average Volume").trim();
            } else {
                avgVolume = "";
                missingStats.add("Average Volume");
            }


            /* Some stocks don't have a description. If there is no description, then
             * descriptionElmnt does not exist. */
            final Element descriptionElmnt = regionPrimary.selectFirst(":root > div.template.template--primary > div.column.column--full > div[class*=description] > p.description__text");
            final String description;
            if (descriptionElmnt != null) {
                /* There is a button at the bottom of the description that is a link to the profile
                 * tab of the individual stock site. The button's title shows up as part of the
                 * text - remove it. */
                description = substringBefore(descriptionElmnt.text(), " (See Full Profile)");
            } else {
                description = "";
                missingStats.add("Description");
            }


            switch (state) {
                case PREMARKET:
                    ret = new PremarketStock(state, mticker, name, price, changePoint, changePercent,
                            close_price, close_changePoint, close_changePercent, openPrice, dayRangeLow,
                            dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh, marketCap,
                            beta, peRatio, eps, yield, avgVolume, description, chart_prices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years);
                    break;
                case OPEN:
                    ret = new AdvancedStock(state, mticker, name, price, changePoint, changePercent,
                            openPrice, dayRangeLow, dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh,
                            marketCap, beta, peRatio, eps, yield, avgVolume, description, chart_prices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years);
                    break;
                case AFTER_HOURS:
                    ret = new AfterHoursStock(state, mticker, name, price, changePoint, changePercent,
                            close_price, close_changePoint, close_changePercent, openPrice, dayRangeLow,
                            dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh, marketCap,
                            beta, peRatio, eps, yield, avgVolume, description, chart_prices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years);
                    break;
                case CLOSED:
                    ret = new AdvancedStock(state, mticker, name, price, changePoint, changePercent,
                            openPrice, dayRangeLow, dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh,
                            marketCap, beta, peRatio, eps, yield, avgVolume, description, chart_prices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years);
                    break;
                default:
                    ret = null;
                    break;
            }

            return ret;
        }

        @Override
        protected void onPostExecute(final AdvancedStock stock) {
            mcompletionListener.get().onDownloadIndividualStockTaskCompleted(stock, missingStats);
        }
    }

    @BindView(R.id.progressBar_individual) ProgressBar mprogressBar;
    @BindView(R.id.sparkView_individual) SparkView msparkView;
    @BindView(R.id.textView_scrubPrice_individual) TextView mscrubPrice;
    @BindView(R.id.textView_scrubChangePercent_individual) TextView mscrubChangePercent;
    @BindView(R.id.horizontalPicker_chartPeriod_individual) HorizontalPicker mchartPeriodPicker;
    @BindView(R.id.view_chartPeriodPickerUnderline_individual) View mchartPeriodPickerUnderline;
    @BindView(R.id.divider_sparkViewToStats_individual) View msparkViewToStatsDivider;
    @BindView(R.id.textView_state_individual) TextView mstate;
    @BindView(R.id.textView_price_individual) TextView mprice;
    @BindView(R.id.textView_changePoint_individual) TextView mchangePoint;
    @BindView(R.id.textView_changePercent_individual) TextView mchangePercent;
    @BindView(R.id.textView_close_price_individual) TextView mclose_price;
    @BindView(R.id.textView_close_changePoint_individual) TextView mclose_changePoint;
    @BindView(R.id.textView_close_changePercent_individual) TextView mclose_changePercent;
    @BindView(R.id.textView_openPrice_individual) TextView mopenPrice;
    @BindView(R.id.textView_dayRange_individual) TextView mdayRange;
    @BindView(R.id.textView_fiftyTwoWeekRange_individual) TextView mfiftyTwoWeekRange;
    @BindView(R.id.textView_marketCap_individual) TextView mmarketCap;
    @BindView(R.id.textView_beta_individual) TextView mbeta;
    @BindView(R.id.textView_peRatio_individual) TextView mpeRatio;
    @BindView(R.id.textView_eps_individual) TextView meps;
    @BindView(R.id.textView_yield_individual) TextView myield;
    @BindView(R.id.textView_averageVolume_individual) TextView mavgVolume;
    @BindView(R.id.divider_statisticsToDescription_individual) View mstatsToDescriptionDivider;
    @BindView(R.id.textView_description_individual) TextView mdescriptionTextView;

    private String mticker; // Needed to create mstock
    private AdvancedStock mstock;
    private boolean mwasInFavoritesInitially;
    private boolean misInFavorites;
    private SparkViewAdapter msparkViewAdapter;
    private SharedPreferences mpreferences;

    /* Called from DownloadStockDataTask.onPostExecute(). */
    @Override
    public void onDownloadIndividualStockTaskCompleted(final AdvancedStock stock, final HashSet missingStats) {
        mstock = stock;

        if (!getTitle().equals(mstock.getName())) {
            setTitle(mstock.getName());
        }

        if (mstock.getState() == OPEN || mstock.getState() == CLOSED) {
            mclose_price.setVisibility(View.GONE);
            mclose_changePoint.setVisibility(View.GONE);
            mclose_changePercent.setVisibility(View.GONE);
        }

        if (!mstock.getYData_1day().isEmpty()) { /** Change: check for available charts using missingStats. */
            msparkViewAdapter.setyData(mstock.getYData_1day());
            msparkViewAdapter.notifyDataSetChanged();
            mscrubPrice.setText(getString(R.string.dollarSign_double2dec, mstock.getPrice())); // Init
            mscrubChangePercent.setText(getString(R.string.double2dec_percent, mstock.getChangePercent())); // Init
            msparkView.setScrubListener((final Object valueObj) -> {
                if (valueObj == null) {
                    // The user is not scrubbing
                    mscrubPrice.setText(getString(R.string.dollarSign_double2dec, mstock.getPrice()));
                    mscrubChangePercent.setText(getString(R.string.double2dec_percent, mstock.getChangePercent()));
                    final int deactivatedColor = getResources().getColor(R.color.colorAccentTransparent, getTheme());
                    mscrubPrice.setTextColor(deactivatedColor);
                    mscrubChangePercent.setTextColor(deactivatedColor);
                } else {
                    // The user is scrubbing
                    // Calculate the percent change at this scrubbing location
                    mscrubPrice.setText(getString(R.string.dollarSign_double2dec, (double) valueObj));
                    final double realPrice = mstock.getPrice();
                    final double curPrice = (double) valueObj;
                    final double curChangePercent = 100 * ((curPrice - realPrice) / realPrice);
                    mscrubChangePercent.setText(getString(R.string.double2dec_percent, curChangePercent));
                    final int activatedColor = getResources().getColor(R.color.colorAccent, getTheme());
                    mscrubPrice.setTextColor(activatedColor);
                    mscrubChangePercent.setTextColor(activatedColor);
                }
            });

            msparkView.setVisibility(View.VISIBLE);
            mchartPeriodPicker.setVisibility(View.VISIBLE);
            mchartPeriodPickerUnderline.setVisibility(View.VISIBLE);
            msparkViewToStatsDivider.setVisibility(View.VISIBLE);
        } else {
            mscrubPrice.setVisibility(View.GONE);
            mscrubChangePercent.setVisibility(View.GONE);
        }

        mstate.setText(getString(R.string.string_colon_string, "State", mstock.getState().toString()));
        mprice.setText(getString(R.string.string_colon_double2dec, "Price", mstock.getPrice()));
        mchangePoint.setText(getString(R.string.string_colon_double2dec, "Point Change", mstock.getChangePoint()));
        mchangePercent.setText(getString(R.string.string_colon_double2dec_percent, "Percent Change", mstock.getChangePercent()));
        if (mstock.getState() == PREMARKET) {
            final PremarketStock ahStock = (PremarketStock) mstock;
            mclose_price.setText(getString(R.string.string_colon_double2dec, "Price at Close", ahStock.getClose_price()));
            mclose_changePoint.setText(getString(R.string.string_colon_double2dec, "Point Change at Close", ahStock.getClose_changePoint()));
            mclose_changePercent.setText(getString(R.string.string_colon_double2dec_percent, "Percent Change at Close", ahStock.getClose_changePercent()));
        } else if (mstock.getState() == AFTER_HOURS) {
            final AfterHoursStock ahStock = (AfterHoursStock) mstock;
            mclose_price.setText(getString(R.string.string_colon_double2dec, "Price at Close", ahStock.getClose_price()));
            mclose_changePoint.setText(getString(R.string.string_colon_double2dec, "Point Change at Close", ahStock.getClose_changePoint()));
            mclose_changePercent.setText(getString(R.string.string_colon_double2dec_percent, "Percent Change at Close", ahStock.getClose_changePercent()));
        }

        if (!missingStats.contains("Price at Open")) {
            mopenPrice.setText(getString(R.string.string_colon_double2dec, "Price at Open", mstock.getOpenPrice()));
        } else {
            mopenPrice.setVisibility(View.GONE);
        }
        if (!missingStats.contains("Day Range")) {
            mdayRange.setText(getString(R.string.string_colon_double2dec_hyphen_double2dec, "Day Range", mstock.getDayRangeLow(), mstock.getDayRangeHigh()));
        } else {
            mdayRange.setVisibility(View.GONE);
        }
        if (!missingStats.contains("52 Week Range")) {
            mfiftyTwoWeekRange.setText(getString(R.string.string_colon_double2dec_hyphen_double2dec, "52 Week Range", mstock.getFiftyTwoWeekRangeLow(), mstock.getFiftyTwoWeekRangeHigh()));
        } else {
            mfiftyTwoWeekRange.setVisibility(View.GONE);
        }
        if (!missingStats.contains("Market Cap")) {
            mmarketCap.setText(getString(R.string.string_colon_string, "MarketCap", mstock.getMarketCap()));
        } else {
            mmarketCap.setVisibility(View.GONE);
        }
        if (!missingStats.contains("Beta")) {
            mbeta.setText(getString(R.string.string_colon_double2dec, "Beta", mstock.getBeta()));
        } else {
            mbeta.setVisibility(View.GONE);
        }
        if (!missingStats.contains("P/E Ratio")) {
            mpeRatio.setText(getString(R.string.string_colon_double2dec, "P/E Ratio", mstock.getPeRatio()));
        } else {
            mpeRatio.setVisibility(View.GONE);
        }
        if (!missingStats.contains("EPS")) {
            meps.setText(getString(R.string.string_colon_double2dec, "EPS", mstock.getEps()));
        } else {
            meps.setVisibility(View.GONE);
        }
        if (!missingStats.contains("Yield")) {
            myield.setText(getString(R.string.string_colon_double2dec, "Yield", mstock.getYield()));
        } else {
            myield.setVisibility(View.GONE);
        }
        if (!missingStats.contains("Average Volume")) {
            mavgVolume.setText(getString(R.string.string_colon_string, "Average Volume", mstock.getAverageVolume()));
        } else {
            mavgVolume.setVisibility(View.GONE);
        }
        if (!missingStats.contains("Description")) {
            mdescriptionTextView.setText(mstock.getDescription());
            mstatsToDescriptionDivider.setVisibility(View.VISIBLE);
        } else {
            mdescriptionTextView.setVisibility(View.GONE);
        }

        mprogressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual_stock);
        setTitle(""); // Show empty title now, company name will be shown (in onPostExecute())
        ButterKnife.bind(this);
        mticker = getIntent().getStringExtra("Ticker");

        // Start task ASAP
        new DownloadStockDataTask(mticker, this).execute();

        mpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        msparkViewAdapter = new SparkViewAdapter(new ArrayList<>()); // Init as empty
        msparkView.setAdapter(msparkViewAdapter);
        mchartPeriodPicker.setOnItemSelectedListener(this);
        misInFavorites = getIntent().getBooleanExtra("Is in favorites", false);
        mwasInFavoritesInitially = misInFavorites;
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
             * The condition to be able to edit Tickers CSV and Data CSV are dependent on whether
             * or not the favorites status has changed. */
            mwasInFavoritesInitially = misInFavorites;
        }
    }

    @Override
    public void onBackPressed() {
        /* The parent (non-override) onBackPressed() does not create a new HomeActivity. So when we
         * go back back to HomeActivity, the first function called is onResume(); onCreate() is not
         * called. HomeActivity depends on the property that Tickers CSV and Data CSV are not
         * changed in between calls to HomeActivity.onPause() and HomeActivity.onResume(). Tickers
         * CSV and Data CSV can be changed within this class. Therefore, if we don't start a new
         * HomeActivity in this function, then it is possible that Tickers CSV and Data CSV are
         * changed in between calls to HomeActivity.onResume() and HomeActivity.onPause(). */
        final Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    /* For mchartPeriodPicker. */
    @Override
    public void onItemSelected(final int index) {
        if (mstock != null) { // In case user selects something before mstock is initialized.
            switch (index) {
                case 0: // 1D
                    if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.ONE_DAY) {
                        msparkViewAdapter.setyData(mstock.getYData_1day());
                        msparkViewAdapter.notifyDataSetChanged();
                        msparkViewAdapter.setMchartPeriod(AdvancedStock.ChartPeriod.ONE_DAY);
                    }
                    break;
                case 1: // 2W
                    if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.TWO_WEEKS) {
                        msparkViewAdapter.setyData(mstock.getYData_2weeks());
                        msparkViewAdapter.notifyDataSetChanged();
                        msparkViewAdapter.setMchartPeriod(AdvancedStock.ChartPeriod.TWO_WEEKS);
                    }
                    break;
                case 2: // 1M
                    if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.ONE_MONTH) {
                        msparkViewAdapter.setyData(mstock.getYData_1month());
                        msparkViewAdapter.notifyDataSetChanged();
                        msparkViewAdapter.setMchartPeriod(AdvancedStock.ChartPeriod.ONE_MONTH);
                    }
                    break;
                case 3: // 3M
                    if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.THREE_MONTHS) {
                        msparkViewAdapter.setyData(mstock.getYData_3months());
                        msparkViewAdapter.notifyDataSetChanged();
                        msparkViewAdapter.setMchartPeriod(AdvancedStock.ChartPeriod.THREE_MONTHS);
                    }
                    break;
                case 4: // 1Y
                    if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.ONE_YEAR) {
                        msparkViewAdapter.setyData(mstock.getYData_1year());
                        msparkViewAdapter.notifyDataSetChanged();
                        msparkViewAdapter.setMchartPeriod(AdvancedStock.ChartPeriod.ONE_YEAR);
                    }
                    break;
                case 5: // 5Y
                    if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.FIVE_YEARS) {
                        msparkViewAdapter.setyData(mstock.getYData_5years());
                        msparkViewAdapter.notifyDataSetChanged();
                        msparkViewAdapter.setMchartPeriod(AdvancedStock.ChartPeriod.FIVE_YEARS);
                    }
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stock_activity, menu);
        final MenuItem starItem = menu.findItem(R.id.starMenuItem);
        starItem.setIcon(misInFavorites ? R.drawable.star_on : R.drawable.star_off);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
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
        final String dataStr;

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
            final ArrayList<String> dataList = new ArrayList<>(Arrays.asList(
                    mpreferences.getString("Data CSV", "").split(",")));

            final int tickerNdx = tickerList.indexOf(mstock.getTicker());
            tickerList.remove(tickerNdx);
            mpreferences.edit().putString("Tickers CSV", String.join(",", tickerList)).apply();

            // 4 data elements per 1 ticker. DataNdx is the index of the first element to delete.
            final int dataNdx = tickerNdx * 4;
            for (int deleteCount = 1; deleteCount <= 4; deleteCount++) { // Delete 4 data elements
                dataList.remove(dataNdx);
            }
            mpreferences.edit().putString("Data CSV", String.join(",", dataList)).apply();
        }
    }
}
