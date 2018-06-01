package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;


public class RecyclerHomeAdapter extends RecyclerView.Adapter<RecyclerHomeAdapter.ViewHolder> {


    public interface OnItemClickListener {
        void onItemClick(HalfStock halfStock);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tickerTextView;
        private TextView priceTextView;
        private TextView priceChangePercentTextView;


        public ViewHolder(View v) {
            super(v);

            tickerTextView = v.findViewById(R.id.textView_ticker_homeRecyclerItem);
            priceTextView = v.findViewById(R.id.textView_price_homeRecyclerItem);
            priceChangePercentTextView = v.findViewById(R.id.textView_priceChangePercent_homeRecyclerItem);
        }


        public void bind(final HalfStock halfStock, final OnItemClickListener listener) {
            tickerTextView.setText(halfStock.getTicker());
            priceTextView.setText(String.format(Locale.US, "%.2f", halfStock.getPrice()));
            // Append '%' onto end of price change percent. First '%' is escape char for '%'.
            priceChangePercentTextView.setText(String.format(Locale.US, "%.2f%%", halfStock.getPriceChangePercent()));

            // Assign green or red color to price change percent text
            if (halfStock.getPriceChangePercent() < 0) {
                priceChangePercentTextView.setTextColor(Color.RED);
            } else {
                priceChangePercentTextView.setTextColor(Color.GREEN);
            }

            itemView.setOnClickListener(l -> listener.onItemClick(halfStock));
        }
    }


    private ArrayList<HalfStock> halfStocks;
    private OnItemClickListener onItemClickListener;


    public RecyclerHomeAdapter(ArrayList<HalfStock> halfStocks, OnItemClickListener listener) {
        this.halfStocks = halfStocks;
        this.onItemClickListener = listener;
    }


    @NonNull
    @Override
    public RecyclerHomeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View stockView = inflater.inflate(R.layout.recycler_item_home_activity, parent, false);

        return new ViewHolder(stockView);
    }


    @NonNull
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(halfStocks.get(position), onItemClickListener);
    }


    @Override
    public int getItemCount() {
        return halfStocks.size();
    }

}