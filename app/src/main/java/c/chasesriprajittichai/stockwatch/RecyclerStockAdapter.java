package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerStockAdapter extends RecyclerView.Adapter<RecyclerStockAdapter.ViewHolder> {


    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView statTextView;


        public ViewHolder(View v) {
            super(v);
            statTextView = v.findViewById(R.id.textView_stockStat_stockRecyclerItem);
        }


        public void bind(final StockStat stockStat) {
            statTextView.setText(stockStat.toString());
        }
    }


    private ArrayList<StockStat> stockStats;


    public RecyclerStockAdapter(ArrayList<StockStat> stockStats) {
        this.stockStats = stockStats;
    }


    @NonNull
    @Override
    public RecyclerStockAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);

        View stockView = inflater.inflate(R.layout.recycler_item_stock_activity, parent, false);

        RecyclerStockAdapter.ViewHolder viewHolder = new RecyclerStockAdapter.ViewHolder(stockView);

        return viewHolder;
    }

    @NonNull
    @Override
    public void onBindViewHolder(@NonNull RecyclerStockAdapter.ViewHolder holder, int position) {
        holder.bind(stockStats.get(position));
    }


    @Override
    public int getItemCount() {
        return stockStats.size();
    }

}
