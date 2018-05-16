package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;


public class RecyclerViewStockAdapter extends RecyclerView.Adapter<RecyclerViewStockAdapter.ViewHolder> {


    public interface onItemClickListener {
        void onItemClick(HalfStock halfStock);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tickerTextView;
        private TextView priceTextView;
        private TextView dailyPriceChangeTextView;

        public ViewHolder(View v) {
            super(v);

            tickerTextView = v.findViewById(R.id.tickerTextView);
            priceTextView = v.findViewById(R.id.priceTextView);
            dailyPriceChangeTextView = v.findViewById(R.id.dailyPriceChangeTextView);
        }


        public void bind(final HalfStock stock, final onItemClickListener listener) {
            tickerTextView.setText(stock.getTicker());
            priceTextView.setText(stock.getStockStat("Price").getValue());
            dailyPriceChangeTextView.setText(stock.getStockStat("Daily Price Change").getValue());

            // Assign green or red color to daily price change text
            if (stock.getStockStat("Daily Price Change").getValue().contains("-")) {
                dailyPriceChangeTextView.setTextColor(Color.RED);
            } else {
                dailyPriceChangeTextView.setTextColor(Color.GREEN);
            }

            itemView.setOnClickListener(l -> listener.onItemClick(stock));
        }
    }


    private ArrayList<HalfStock> stockList;
    private onItemClickListener listener;


    public RecyclerViewStockAdapter(ArrayList<HalfStock> stockList, onItemClickListener listener) {
        this.stockList = stockList;
        this.listener = listener;
    }


    @NonNull
    @Override
    public RecyclerViewStockAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);

        View stockView = inflater.inflate(R.layout.recycler_view_item, parent, false);

        ViewHolder viewHolder = new ViewHolder(stockView);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(stockList.get(position), listener);
    }


    @Override
    public int getItemCount() {
        return stockList.size();
    }

}