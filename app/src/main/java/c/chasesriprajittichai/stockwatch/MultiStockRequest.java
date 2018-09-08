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

import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithEhValsList;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithEhVals;
import c.chasesriprajittichai.stockwatch.stocks.Stock;

import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.PREMARKET;
import static java.lang.Double.parseDouble;


/**
 * Modeled after {@link com.android.volley.toolbox.StringRequest}.
 */
public final class MultiStockRequest extends Request<ConcreteStockWithEhValsList> {

    /**
     * Lock to guard {@link #responseListener} as it is cleared on {@link
     * #cancel()} and read on delivery.
     */
    private final Object lock = new Object();

    /**
     * Guarded by {@link #lock}.
     */
    private Response.Listener<ConcreteStockWithEhValsList> responseListener;

    /**
     * This MultiStockRequest's ConcreteStockWithEhVals with a maximum size of
     * 10. This reference contains the same ConcreteStockWithEhVals that are in
     * {@link HomeActivity#stocks}. This way, we can update the
     * ConcreteStockWithEhVals in HomeActivity.
     */
    private final ConcreteStockWithEhValsList stocks;

    /**
     * @param url              The URL of the MarketWatch multiple-stock site to
     *                         get data from
     * @param stocks           The stocks that should be updated
     * @param responseListener Listener to receive the
     *                         ConcreteStockWithEhValsList response
     * @param errorListener    Error responseListener, or null to ignore errors
     */
    MultiStockRequest(final String url, final ConcreteStockWithEhValsList stocks,
                      final Response.Listener<ConcreteStockWithEhValsList> responseListener,
                      final Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.responseListener = responseListener;
        this.stocks = new ConcreteStockWithEhValsList(stocks);
    }

    /**
     * This method parses the HTML response of the MarketWatch multiple-stock
     * website that displays up to 10 stocks. From the response, the fields
     * defined in {@link Stock} are retrieved for each of the stocks represented
     * in the website/response. Each {@link ConcreteStockWithEhVals} in
     * {@link #stocks} is then updated with the parsed information.
     * <p>
     * This method will be called from a worker thread.
     *
     * @param response Response from the network
     * @return The parsed {@code Response<ConcreteStockWithEhValsList>}, or null
     * in the case of an error
     */
    @Override
    protected Response<ConcreteStockWithEhValsList> parseNetworkResponse(final NetworkResponse response) {
        ConcreteStockWithEhVals curStock;
        Stock.State curState;
        double curPrice, curChangePoint, curChangePercent,
                curEhPrice, curEhChangePoint, curEhChangePercent;
        final Elements quoteRoots, live_valueRoots, tickers, states, live_prices,
                live_changeRoots, live_changePoints, live_changePercents, close_valueRoots,
                close_prices, close_changeRoots, close_changePoints, close_changePercents;

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

                curEhPrice = parseDouble(
                        live_prices.get(i).ownText().replaceAll("[^0-9.]+", ""));
                curEhChangePoint = parseDouble(
                        live_changePoints.get(i).ownText().replaceAll("[^0-9.-]+", ""));
                curEhChangePercent = parseDouble(
                        live_changePercents.get(i).ownText().replaceAll("[^0-9.-]+", ""));
            } else {
                curPrice = parseDouble(
                        live_prices.get(i).ownText().replaceAll("[^0-9.]+", ""));
                curChangePoint = parseDouble(
                        live_changePoints.get(i).ownText().replaceAll("[^0-9.-]+", ""));
                curChangePercent = parseDouble(
                        live_changePercents.get(i).ownText().replaceAll("[^0-9.-]+", ""));

                // Ensure that extra hours values are 0
                curEhPrice = 0;
                curEhChangePoint = 0;
                curEhChangePercent = 0;
            }

            curStock.setPrice(curPrice);
            curStock.setChangePoint(curChangePoint);
            curStock.setChangePercent(curChangePercent);
            curStock.setExtraHoursPrice(curEhPrice);
            curStock.setExtraHoursChangePoint(curEhChangePoint);
            curStock.setExtraHoursChangePercent(curEhChangePercent);
        }

        return Response.success(stocks, HttpHeaderParser.parseCacheHeaders(response));
    }

    /**
     * Callback method to {@link #responseListener}. Pass the update {@link
     * #stocks} to the responseListener as a parameter.
     *
     * @param responseStocks The ConcreteStockWithEhValsList to pass to
     *                       responseListener; same as {@link #stocks}
     */
    @Override
    protected void deliverResponse(final ConcreteStockWithEhValsList responseStocks) {
        final Response.Listener<ConcreteStockWithEhValsList> listener;
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
