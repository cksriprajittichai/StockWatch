package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class Stock {

    private static int NUM_STATS;

    // Init as base URL
    private String urlStr = "https://finance.yahoo.com/quote/<STOCK_TICKER>/key-statistics?p=<STOCK_TICKER>";

    private String ticker;
    private ArrayList<String> statNames;
    private ArrayList<StockStat> stockStats;


    public Stock(Context context, String ticker) {
        this.ticker = ticker;
        urlStr = urlStr.replace("<STOCK_TICKER>", ticker);

        // Init statNames. Context is vital.
        String[] tempStatNames = context.getResources().getStringArray(R.array.statsBeingUsed);
        NUM_STATS = tempStatNames.length;
        statNames = new ArrayList<>(NUM_STATS);
        stockStats = new ArrayList<>(NUM_STATS);
        for (String s : tempStatNames) {
            statNames.add(s);
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

            Elements nonDailyElements = doc.select("tr > td");
            // Add dynamically gathered stats (all stats except daily price change)
            int currentStatNdx = 2; // Starts at market cap (intraday)
            for (int i = 0; i < nonDailyElements.size(); i++) {
                if (nonDailyElements.get(i).text().startsWith(statNames.get(currentStatNdx))) {
                    // Numeric values correspond to the stat at the prior index.
                    // Append numeric value to statName.
                    StockStat tempStockStat = new StockStat(statNames.get(currentStatNdx), nonDailyElements.get(i + 1).text());
                    stockStats.add(tempStockStat);

                    if (currentStatNdx < NUM_STATS - 1) {
                        currentStatNdx++;
                    } else {
                        break; // No more stats needed
                    }
                }
            }
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
     *
     * @param statName
     * @return Null if a stock with statName is not found in stats.
     */
    public StockStat getStockStat(String statName) {
        for (StockStat ss : stockStats) {

            if (ss.getName().equals(statName)) {
                return ss;
            }
        }

        return null;
    }

}
