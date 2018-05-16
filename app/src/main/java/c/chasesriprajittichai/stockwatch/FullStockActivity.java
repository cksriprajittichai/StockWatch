package c.chasesriprajittichai.stockwatch;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class FullStockActivity extends AppCompatActivity {

    private FullStock fullStock;

    private class DownloadStockStatsTask extends AsyncTask<String, Integer, FullStock> {

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

//            TextView priceTextView = findViewById(R.id.priceTextView);
//            priceTextView.setText(fullStock.getStockStat("Price").getValue());
//            TextView dailyPriceChangeTextView = findViewById(R.id.dailyPriceChangeTextView);
//            dailyPriceChangeTextView.setText(fullStock.getStockStat("Daily Price Change").getValue());
//
//            // Assign green or red color to daily price change text
//            if (fullStock.getStockStat("Daily Price Change").getValue().contains("-")) {
//                dailyPriceChangeTextView.setTextColor(Color.RED);
//            } else {
//                dailyPriceChangeTextView.setTextColor(Color.GREEN);
//            }

            int numStockStats = fullStock.getStockStats().size();
            ArrayList<StockStat> stockStats = fullStock.getStockStats();
            String[] stockStatToStrings = new String[numStockStats];
            for (int i = 0; i < numStockStats; i++) {
                stockStatToStrings[i] = stockStats.get(i).toString();
            }

            listView = findViewById(R.id.listView);
            listAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, stockStatToStrings) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    view.setPadding(16, 4, 16, 4);

                    TextView textView = view.findViewById(android.R.id.text1);
                    textView.setText(stockStatToStrings[position]);

                    textView.setTextSize(24);
                    textView.setTextColor(Color.WHITE);

                    return view;
                }
            };
            listView.setAdapter(listAdapter);
        }
    }


    private ListView listView;
    private ListAdapter listAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(""); // Clear title until company name is shown (in onPostExecute())
        setContentView(R.layout.activity_full_stock);

        new DownloadStockStatsTask().execute(getIntent().getStringExtra("Ticker"));
    }

}
