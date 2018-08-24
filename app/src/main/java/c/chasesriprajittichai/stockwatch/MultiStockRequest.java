package c.chasesriprajittichai.stockwatch;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.Locale;

import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithAhValsList;
import c.chasesriprajittichai.stockwatch.stocks.Stock;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithAhVals;

import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.PREMARKET;
import static java.lang.Double.parseDouble;


/**
 * Modeled after {@link com.android.volley.toolbox.StringRequest}.
 */
public final class MultiStockRequest extends Request<ConcreteStockWithAhValsList> {

    /**
     * Lock to guard {@link #responseListener} as it is cleared on {@link
     * #cancel()} and read on delivery.
     */
    private final Object lock = new Object();

    /**
     * Guarded by {@link #lock}.
     */
    private Response.Listener<ConcreteStockWithAhValsList> responseListener;

    /**
     * This MultiStockRequest's ConcreteStockWithAhVals with a maximum size of
     * 10. This reference contains the same ConcreteStockWithAhVals that are in
     * {@link HomeActivity#stocks}. This way, we can update the
     * ConcreteStockWithAhVals in HomeActivity.
     */
    private final ConcreteStockWithAhValsList stocks;

    /**
     * @param url              The URL of the MarketWatch multiple-stock site to
     *                         get data from
     * @param stocks           The stocks that should be updated
     * @param responseListener Listener to receive the
     *                         ConcreteStockWithAhValsList response
     * @param errorListener    Error responseListener, or null to ignore errors
     */
    MultiStockRequest(final String url, final ConcreteStockWithAhValsList stocks,
                      final Response.Listener<ConcreteStockWithAhValsList> responseListener,
                      final Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.responseListener = responseListener;
        this.stocks = new ConcreteStockWithAhValsList(stocks);
    }

    /**
     * This method parses the HTML response of the MarketWatch multiple-stock
     * website that displays up to 10 stocks. From the response, the fields
     * defined in {@link Stock} are retrieved for each of the stocks represented
     * in the website/response. Each {@link ConcreteStockWithAhVals} in
     * {@link #stocks} is then updated with the parsed information.
     * <p>
     * This method will be called from a worker thread.
     *
     * @param response Response from the network
     * @return The parsed {@code Response<ConcreteStockWithAhValsList>}, or null
     * in the case of an error
     */
    @Override
    protected Response<ConcreteStockWithAhValsList> parseNetworkResponse(final NetworkResponse response) {
        ConcreteStockWithAhVals curStock;
        Stock.State curState;
        double curPrice, curChangePoint, curChangePercent,
                curAhPrice, curAhChangePoint, curAhChangePercent;
        final Elements quoteRoots, live_valueRoots, tickers, states, live_prices,
                live_changeRoots, live_changePoints, live_changePercents, close_valueRoots,
                close_prices, close_changeRoots, close_changePoints, close_changePercents;

        /* Prices and changes gathered now are the current values. For example,
         * if a stock is in the AFTER_HOURS state, then it's price, change
         * point, and change percent will be the stock's current price, after
         * hours change point, and after hours change percent. */

        final Document doc = Jsoup.parse(new String(response.data));
        quoteRoots = doc.select(
                ":root > body > div[id=blanket] > div[class*=multi] > " +
                        "div[id=maincontent] > div[class^=block multiquote] > " +
                        "div[class^=quotedisplay]");

        live_valueRoots = quoteRoots.select(
                ":root > div[class^=section activeQuote bgQuote]");
        tickers = live_valueRoots.select(
                ":root > div.ticker > a[href][title]");
        states = live_valueRoots.select(
                ":root > div.marketheader > p.column.marketstate");
        live_prices = live_valueRoots.select(
                ":root > div.lastprice > div.pricewrap > p.data.bgLast");
        live_changeRoots = live_valueRoots.select(
                ":root > div.lastpricedetails > p.lastcolumn.data");
        live_changePoints = live_changeRoots.select(
                ":root > span.bgChange");
        live_changePercents = live_changeRoots.select(
                ":root > span.bgPercentChange");

        close_valueRoots = quoteRoots.select(
                ":root > div[class^=prevclose section bgQuote] > div.offhours");
        close_prices = close_valueRoots.select(
                ":root > p.lastcolumn.data.bgLast.price");
        close_changeRoots = close_valueRoots.select(
                ":root > p.lastcolumn.data");
        close_changePoints = close_changeRoots.select(
                ":root > span.bgChange");
        close_changePercents = close_changeRoots.select(
                ":root > span.bgPercentChange");

        final int numStocksToUpdate = tickers.size();

        // Iterate through stocks that we're updating
        for (int i = 0; i < numStocksToUpdate; i++) {
            switch (states.get(i).ownText().toLowerCase(Locale.US)) {
                case "before the bell":
                    curState = PREMARKET;
                    break;
                case "market open":
                case "countdown to close":
                    curState = OPEN;
                    break;
                case "after hours":
                    curState = AFTER_HOURS;
                    break;
                case "market closed":
                    curState = CLOSED;
                    break;
                default:
                    Log.e("UnrecognizedMarketWatchState", String.format(
                            "Unrecognized state string from Market Watch multiple stock page.%n" +
                                    "Unrecognized state string: %s%n" +
                                    "Ticker: %s", states.get(i).ownText(), tickers.get(i).ownText()));
                    // Do not add this error stock to the list that will be returned
                    continue;
            }

            curStock = stocks.get(i);
            curStock.setState(curState);

            // Remove ',' or '%' that could be in strings
            if (curState == AFTER_HOURS || curState == PREMARKET) {
                curPrice = parseDouble(
                        close_prices.get(i).ownText().replaceAll("[^0-9.]+", ""));
                curChangePoint = parseDouble(
                        close_changePoints.get(i).ownText().replaceAll("[^0-9.-]+", ""));
                curChangePercent = parseDouble(
                        close_changePercents.get(i).ownText().replaceAll("[^0-9.-]+", ""));

                curAhPrice = parseDouble(
                        live_prices.get(i).ownText().replaceAll("[^0-9.]+", ""));
                curAhChangePoint = parseDouble(
                        live_changePoints.get(i).ownText().replaceAll("[^0-9.-]+", ""));
                curAhChangePercent = parseDouble(
                        live_changePercents.get(i).ownText().replaceAll("[^0-9.-]+", ""));
            } else {
                curPrice = parseDouble(
                        live_prices.get(i).ownText().replaceAll("[^0-9.]+", ""));
                curChangePoint = parseDouble(
                        live_changePoints.get(i).ownText().replaceAll("[^0-9.-]+", ""));
                curChangePercent = parseDouble(
                        live_changePercents.get(i).ownText().replaceAll("[^0-9.-]+", ""));

                // Ensure that after hours values are 0
                curAhPrice = 0;
                curAhChangePoint = 0;
                curAhChangePercent = 0;
            }

            curStock.setPrice(curPrice);
            curStock.setChangePoint(curChangePoint);
            curStock.setChangePercent(curChangePercent);
            curStock.setAfterHoursPrice(curAhPrice);
            curStock.setAfterHoursChangePoint(curAhChangePoint);
            curStock.setAfterHoursChangePercent(curAhChangePercent);
        }

        return Response.success(stocks, HttpHeaderParser.parseCacheHeaders(response));
    }

    /**
     * Callback method to {@link #responseListener}. Pass the update {@link
     * #stocks} to the responseListener as a parameter.
     *
     * @param responseStocks The ConcreteStockWithAhValsList to pass to
     *                       responseListener; same as {@link #stocks}
     */
    @Override
    protected void deliverResponse(final ConcreteStockWithAhValsList responseStocks) {
        final Response.Listener<ConcreteStockWithAhValsList> listener;
        synchronized (lock) {
            listener = this.responseListener;
        }
        if (listener != null) {
            listener.onResponse(responseStocks);
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        synchronized (lock) {
            responseListener = null;
        }
    }

}
