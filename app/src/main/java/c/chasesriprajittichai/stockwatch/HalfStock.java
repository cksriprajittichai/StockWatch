package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class HalfStock {

    private static final int NUM_STATS = 2; // Price and daily price change

    // Init as base URL
    private String urlStr = "https://finance.yahoo.com/quote/<STOCK_TICKER>/key-statistics?p=<STOCK_TICKER>";

    private String ticker;
    private ArrayList<String> statNames = new ArrayList<>(NUM_STATS);
    private ArrayList<StockStat> stockStats = new ArrayList<>(NUM_STATS);


    public HalfStock(Context context, String ticker) {
        this.ticker = ticker;
        urlStr = urlStr.replace("<STOCK_TICKER>", ticker);

        // Init statNames. Context is vital. Only add first two elements of tempStatNames.
        // Price and daily price change.
        String[] tempStatNames = context.getResources().getStringArray(R.array.statsBeingUsed);
        for (int i = 0; i < NUM_STATS; i++) {
            statNames.add(tempStatNames[i]);
        }

        initStats();
    }


    private void initStats() {
        try {
            Document doc = Jsoup.connect(getURL().toString()).get();

            // Add price stat
            Element price = doc.selectFirst("span[class=Trsdu(0.3s) Fw(b) Fz(36px) Mb(-4px) D(ib)]");
            stockStats.add(new StockStat(statNames.get(0), price.text()));

            // Add daily price change stat
            Element dailyPriceChange = doc.selectFirst("span[class^=Trsdu(0.3s) Fw(500) Pstart(10px) Fz(24px)]");
            stockStats.add(new StockStat(statNames.get(1), dailyPriceChange.text()));

            /**
             * AFTER HOURS? Website changes at midnight? - after hours didn't work at 12:15am.
             */

        } catch (MalformedURLException murle) {
            Log.e("CHASE ERROR", murle.getMessage());
        } catch (IOException ioe) {
            Log.e("CHASE ERROR", ioe.getMessage());
        }
    }


    /**
     * Creates this stock's URL for Yahoo Finance statistics page using the BASE_URL and ticker.
     * Replaces inserts in BASE_URL with ticker.
     *
     * @return URL for this stock; null if ticker is empty or null.
     * @throws MalformedURLException
     */
    private URL getURL() throws MalformedURLException {
        return new URL(urlStr);
    }


    public String getTicker() {
        return ticker;
    }

    public ArrayList<StockStat> getStockStats() {
        return stockStats;
    }


    /**
     * @param statName
     * @return Null if a stock with statName is not found in stats.
     */
    public StockStat getStockStat(String statName) {
        for (StockStat ss : stockStats) {
            if (ss.getName().equalsIgnoreCase(statName)) {
                return ss;
            }
        }

        return null;
    }

}
