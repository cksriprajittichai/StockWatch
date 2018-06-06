package c.chasesriprajittichai.stockwatch;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import c.chasesriprajittichai.stockwatch.Stocks.BasicStock;


public class RecyclerHomeAdapter extends RecyclerView.Adapter<RecyclerHomeAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(BasicStock basicStock);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mtickerTextView;
        private TextView mpriceTextView;
        private TextView mpriceChangePercentTextView;

        public ViewHolder(View v) {
            super(v);

            mtickerTextView = v.findViewById(R.id.textView_ticker_homeRecyclerItem);
            mpriceTextView = v.findViewById(R.id.textView_price_homeRecyclerItem);
            mpriceChangePercentTextView = v.findViewById(R.id.textView_priceChangePercent_homeRecyclerItem);
        }

        public void bind(final BasicStock basicStock, final OnItemClickListener listener) {
            mtickerTextView.setText(basicStock.getTicker());
            mpriceTextView.setText(String.format(Locale.US, "%.2f", basicStock.getPrice()));
            // Append '%' onto end of mprice change percent. First '%' is escape char for '%'.
            mpriceChangePercentTextView.setText(String.format(Locale.US, "%.2f%%", basicStock.getChangePercent()));

            // Assign green or red color to mprice change percent text
            if (basicStock.getChangePercent() < 0) {
                mpriceChangePercentTextView.setTextColor(Color.RED);
            } else {
                mpriceChangePercentTextView.setTextColor(Color.GREEN);
            }

            itemView.setOnClickListener(l -> listener.onItemClick(basicStock));
        }
    }

    private ArrayList<BasicStock> mstocks;
    private OnItemClickListener monItemClickListener;

    public RecyclerHomeAdapter(ArrayList<BasicStock> stocks, OnItemClickListener listener) {
        this.mstocks = stocks;
        this.monItemClickListener = listener;
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
        holder.bind(mstocks.get(position), monItemClickListener);
    }

    @Override
    public int getItemCount() {
        return mstocks.size();
    }

}