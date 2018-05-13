package c.chasesriprajittichai.stockwatch;

import android.os.AsyncTask;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;


public class MainActivity extends AppCompatActivity {

    private class DownloadStockStatsTask extends AsyncTask<String, Integer, ArrayList<Stock>> {

        /**
         * Takes tickers as parameters, updates stockList to contain the stocks with tickers.
         *
         * @param tickers Tickers of the stocks to put into stockList.
         * @return stockList, updated with stocks with designated tickers.
         */
        @Override
        protected ArrayList<Stock> doInBackground(String... tickers) {

            // Sort tickers alphabetically by ticker
            ArrayList<String> tempTickers = new ArrayList<>(tickers.length);
            for (String ticker : tickers) {
                tempTickers.add(ticker);
            }
            Collections.sort(tempTickers);
            for (int i = 0; i < tempTickers.size(); i++) {
                tickers[i] = tempTickers.get(i);
            }

            Looper.prepare(); // Vital

            for (String s : tickers) {
                stockList.add(new Stock(getApplicationContext(), s));
            }

            return stockList;
        }

        protected void onPostExecute(ArrayList<Stock> stockList) {
            recyclerView = findViewById(R.id.recycler_view);

            recyclerView.setAdapter(new StockAdapter(stockList, l -> {
                // DO SOMETHING
            }));
            recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            recyclerView.addItemDecoration(new RecyclerViewDivider(getApplicationContext()));

            for (Stock stock : stockList) {
                StringBuilder sb = new StringBuilder();

                sb.append("STOCK >>> " + stock.getTicker() + "\n");
                for (StockStat stockStat : stock.getStockStats()) {
                    sb.append(stockStat.toString() + '\n');
                }
                Log.i("Chase", sb.toString());
            }
        }
    }


    private RecyclerView recyclerView;
    private ArrayList<Stock> stockList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Stock Watch");

//        new DownloadStockStatsTask().execute("BAC", "BA", "FB", "GE", "GOOGL", "GM", "GS", "HD",
//                "IBM", "JPM", "JNJ", "MSFT", "MRK", "AAPL", "INTC", "FSLR", "CAT", "RTN", "DKS",
//                "AAL", "DWDP", "DAL", "CVX", "DRYS", "AMD");
        new DownloadStockStatsTask().execute("BAC", "BA", "FB", "GE", "GOOGL");
    }

}
