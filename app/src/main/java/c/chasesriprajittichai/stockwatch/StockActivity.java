package c.chasesriprajittichai.stockwatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class StockActivity extends AppCompatActivity {


    private static class DownloadFullStockTask extends AsyncTask<String, Integer, Integer> {

        private WeakReference<Context> context;
        private WeakReference<Activity> parentActivity;
        private WeakReference<RecyclerView> recyclerView;
        private WeakReference<FullStock> fullStock;


        private DownloadFullStockTask(Context context, Activity parentActivity, RecyclerView recyclerView) {
            this.context = new WeakReference<>(context);
            this.parentActivity = new WeakReference<>(parentActivity);
            this.recyclerView = new WeakReference<>(recyclerView);
        }


        @Override
        protected Integer doInBackground(String... tickers) {
            fullStock = new WeakReference<>(new FullStock(context.get(), tickers[0]));
            return 0;
        }

        @Override
        protected void onPostExecute(Integer status) {
            parentActivity.get().setTitle(fullStock.get().getCompanyName());
            recyclerView.get().setAdapter(new RecyclerStockAdapter(fullStock.get().getStockStats()));
        }
    }

    private String ticker;
    private boolean isInFavorites;
    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(""); // Show empty title now, company name will be shown (in onPostExecute())
        setContentView(R.layout.activity_stock);
        ticker = getIntent().getStringExtra("Ticker");
        isInFavorites = getIntent().getBooleanExtra("Is in favorites", false);

        // Init recycler view. It is empty now, will be filled in onPostExecute().
        recyclerView = findViewById(R.id.recycler_view_stock);
        recyclerView.setAdapter(new RecyclerStockAdapter(new ArrayList<>())); // Set to empty adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerDivider(this));

        DownloadFullStockTask task = new DownloadFullStockTask(this, this, recyclerView);
        task.execute(ticker);
    }


    /* Back button is the only way to get back to HomeActivity. */
    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("Ticker", ticker);
        returnIntent.putExtra("Is in favorites", isInFavorites);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stock_activity, menu);

        if (isInFavorites) {
            menu.findItem(R.id.starMenuItem).setIcon(R.drawable.star_on);
        } else {
            menu.findItem(R.id.starMenuItem).setIcon(R.drawable.star_off);
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.starMenuItem:
                isInFavorites = !isInFavorites; // Toggle
                if (isInFavorites) {
                    item.setIcon(R.drawable.star_on);
                } else {
                    item.setIcon(R.drawable.star_off);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
