package c.chasesriprajittichai.stockwatch;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.Locale;

import c.chasesriprajittichai.stockwatch.stocks.BasicStock;
import c.chasesriprajittichai.stockwatch.stocks.BasicStockList;

import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.PREMARKET;
import static java.lang.Double.parseDouble;


/**
 * Modeled after com.android.volley.toolbox.StringRequest.
 */
public final class MultiStockRequest extends Request<BasicStockList> {

    /**
     * Lock to guard mListener as it is cleared on cancel() and read on delivery.
     */
    private final Object mLock = new Object();

    // Guarded by mLock
    private Response.Listener<BasicStockList> mlistener;

    private final BasicStockList mstocks;

    /**
     * @param url           The URL of the multiple-stock site to get data from.
     * @param stocks        The stocks that should be updated.
     * @param listener      Listener to receive the BasicStockList response.
     * @param errorListener Error listener, or null to ignore errors.
     */
    MultiStockRequest(final String url, final BasicStockList stocks, final Response.Listener<BasicStockList> listener,
                      final Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        mlistener = listener;
        mstocks = new BasicStockList(stocks);
    }

    /* Does not run on the UI thread. */
    @Override
    protected Response<BasicStockList> parseNetworkResponse(final NetworkResponse response) {
        BasicStock curStock;
        BasicStock.State curState;
        double curPrice, curChangePoint, curChangePercent;
        final Elements quoteRoots, live_valueRoots, tickers, states, live_prices, live_changeRoots,
                live_changePoints, live_changePercents, close_valueRoots, close_prices,
                close_changeRoots, close_changePoints, close_changePercents;

        /* Prices and changes gathered now are the current values. For example, if a stock is in
         * the AFTER_HOURS state, then it's price, change point, and change percent will be the
         * stock's current price, after hours change point, and after hours change percent. */

        final Document doc = Jsoup.parse(new String(response.data));
        quoteRoots = doc.select(":root > body > div[id=blanket] > div[class*=multi] > div[id=maincontent] > div[class^=block multiquote] > div[class^=quotedisplay]");

        live_valueRoots = quoteRoots.select(":root > div[class^=section activeQuote bgQuote]");
        tickers = live_valueRoots.select(":root > div.ticker > a[href][title]");
        states = live_valueRoots.select(":root > div.marketheader > p.column.marketstate");
        live_prices = live_valueRoots.select(":root > div.lastprice > div.pricewrap > p.data.bgLast");
        live_changeRoots = live_valueRoots.select(":root > div.lastpricedetails > p.lastcolumn.data");
        live_changePoints = live_changeRoots.select(":root > span.bgChange");
        live_changePercents = live_changeRoots.select(":root > span.bgPercentChange");

        close_valueRoots = quoteRoots.select(":root > div[class^=prevclose section bgQuote] > div.offhours");
        close_prices = close_valueRoots.select(":root > p.lastcolumn.data.bgLast.price");
        close_changeRoots = close_valueRoots.select(":root > p.lastcolumn.data");
        close_changePoints = close_changeRoots.select(":root > span.bgChange");
        close_changePercents = close_changeRoots.select(":root > span.bgPercentChange");

        final int numStocksToUpdate = tickers.size();

        /* curPrice will be displayed on mrecyclerView. If a stock's state is OPEN or CLOSED, then
         * its display price should be the live price. If a stock's state is PREMARKET OR
         * AFTER_HOURS, then its display price should be the last price that the stock closed at.
         * The same logic applies for curChangePoint and curChangePercent. */
        boolean curDataShouldBeCloseData;

        // Iterate through mstocks that we're updating
        for (int i = 0; i < numStocksToUpdate; i++) {
            switch (states.get(i).text().toLowerCase(Locale.US)) {
                case "premarket": // Individual stock site uses this
                case "before the bell": // Multiple stock view site uses this
                    curState = PREMARKET;
                    curDataShouldBeCloseData = true;
                    break;
                case "countdown to close":
                case "open":
                    curState = OPEN;
                    curDataShouldBeCloseData = false;
                    break;
                case "after hours":
                    curState = AFTER_HOURS;
                    curDataShouldBeCloseData = true;
                    break;
                case "market closed": // Multiple stock view site uses this
                case "closed":
                    curState = CLOSED;
                    curDataShouldBeCloseData = false;
                    break;
                default:
                    curState = OPEN; /** Create error case. */
                    curDataShouldBeCloseData = false;
                    break;
            }

            // Remove ',' or '%' that could be in strings
            if (curDataShouldBeCloseData) {
                curPrice = parseDouble(close_prices.get(i).text().replaceAll("[^0-9.]+", ""));
                curChangePoint = parseDouble(close_changePoints.get(i).text().replaceAll("[^0-9.-]+", ""));
                curChangePercent = parseDouble(close_changePercents.get(i).text().replaceAll("[^0-9.-]+", ""));
            } else {
                curPrice = parseDouble(live_prices.get(i).text().replaceAll("[^0-9.]+", ""));
                curChangePoint = parseDouble(live_changePoints.get(i).text().replaceAll("[^0-9.-]+", ""));
                curChangePercent = parseDouble(live_changePercents.get(i).text().replaceAll("[^0-9.-]+", ""));
            }

            /* mstocks points to the same BasicStock objects that HomeActivity has references to.
             * So this updates the stocks in HomeActivity's mstocks also. */
            curStock = mstocks.get(i);
            curStock.setState(curState);
            curStock.setPrice(curPrice);
            curStock.setChangePoint(curChangePoint);
            curStock.setChangePercent(curChangePercent);
        }

        return Response.success(mstocks, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(final BasicStockList responseStocks) {
        final Response.Listener<BasicStockList> listener;
        synchronized (mLock) {
            listener = mlistener;
        }
        if (listener != null) {
            listener.onResponse(responseStocks);
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        synchronized (mLock) {
            mlistener = null;
        }
    }

}
