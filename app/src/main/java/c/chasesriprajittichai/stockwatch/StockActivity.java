package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;


public class StockActivity extends AppCompatActivity {

    private FullStock fullStock;

    private class DownloadStockStatsTask extends AsyncTask<String, Integer, FullStock> {

        private Context context;

        private DownloadStockStatsTask(Context context) {
            this.context = context;
        }


        /**
         * Takes tickers as parameters, updates stockList to contain the stocks with tickers.
         *
         * @param tickers Tickers of the stocks to put into stockList. There should only be one
         *                ticker in tickers.
         * @return stockList, updated with stocks with designated tickers.
         */
        @Override
        protected FullStock doInBackground(String... tickers) {
            fullStock = new FullStock(getApplicationContext(), tickers[0]);
            return fullStock;
        }


        protected void onPostExecute(FullStock fullStock) {
            setTitle(fullStock.getCompanyName());

            recyclerView.setAdapter(new RecyclerStockAdapter(fullStock.getStockStats()));
        }
    }


    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(""); // Show empty title now, company name will be shown (in onPostExecute())
        setContentView(R.layout.activity_stock);

        // Init recycler view. It is empty now, will be filled in onPostExecute().
        recyclerView = findViewById(R.id.recycler_view_stock);
        recyclerView.setAdapter(new RecyclerStockAdapter(new ArrayList<>())); // Set to empty adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerDivider(this));

        new DownloadStockStatsTask(this).execute(getIntent().getStringExtra("Ticker"));
    }

}
