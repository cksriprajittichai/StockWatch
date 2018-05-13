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
import java.util.HashMap;
import java.util.Map;


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
            try {
                if (tickers[0].startsWith("D")) {
                    Thread.sleep(10000);
                }
            } catch (Exception e) {

            }

            for (String ticker : tickers) {
                stockList.add(new Stock(getApplicationContext(), ticker));
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
        }
    }


    private RecyclerView recyclerView;
    private ArrayList<Stock> stockList = new ArrayList<>();
    private DownloadStockStatsTask[] asyncTasks = new DownloadStockStatsTask[5];
    private HashMap<Integer, Boolean> openTasksMap = new HashMap<Integer, Boolean>() {
        {
            /* All five DownloadStockStatsTasks are open (not being used) */
            for (int taskNdx = 0; taskNdx < asyncTasks.length; taskNdx++) {
                put(taskNdx, true);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Stock Watch");

//        new DownloadStockStatsTask().execute("BAC", "BA", "FB", "GE", "GOOGL", "GM", "GS", "HD",
//                "IBM", "JPM", "JNJ", "MSFT", "MRK", "AAPL", "INTC", "FSLR", "CAT", "RTN", "DKS",
//                "AAL", "DWDP", "DAL", "CVX", "DRYS", "AMD");
//        new DownloadStockStatsTask().execute("BAC", "BA", "FB", "GE", "GOOGL");
        asyncTasks[0] = new DownloadStockStatsTask();
        asyncTasks[0].execute("MRK", "GE", "GM", "DWDP");
        asyncTasks[1] = new DownloadStockStatsTask();
        asyncTasks[1].execute("BA");
        asyncTasks[2] = new DownloadStockStatsTask();
        asyncTasks[2].execute("DPZ");
        asyncTasks[3] = new DownloadStockStatsTask();
        asyncTasks[3].execute("RTN");
        asyncTasks[4] = new DownloadStockStatsTask();
        asyncTasks[4].execute("AAL");
    }

}
