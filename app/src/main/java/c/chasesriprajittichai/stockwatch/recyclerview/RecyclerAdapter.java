package c.chasesriprajittichai.stockwatch.recyclerview;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.Locale;

import c.chasesriprajittichai.stockwatch.R;
import c.chasesriprajittichai.stockwatch.stocks.BasicStock;
import c.chasesriprajittichai.stockwatch.stocks.BasicStockList;


public final class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(final BasicStock basicStock);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView mstateTextView;
        private final TextView mtickerTextView;
        private final TextView mpriceTextView;
        private final TextView mchangePercentTextView;

        ViewHolder(final View v) {
            super(v);

            mstateTextView = v.findViewById(R.id.textView_state_homeRecyclerItem);
            mtickerTextView = v.findViewById(R.id.textView_ticker_homeRecyclerItem);
            mpriceTextView = v.findViewById(R.id.textView_price_homeRecyclerItem);
            mchangePercentTextView = v.findViewById(R.id.textView_changePercent_homeRecyclerItem);
        }

        void bind(final BasicStock stock, final OnItemClickListener listener) {
            if (stock.getState() == BasicStock.State.AFTER_HOURS) {
                // After hours state is the only state with an unwanted character in the enum name
                mstateTextView.setText(String.format(Locale.US, "%s", "AFTER HOURS"));
            } else {
                mstateTextView.setText(stock.getState().toString());
            }
            mtickerTextView.setText(stock.getTicker());
            mpriceTextView.setText(String.format(Locale.US, "%.2f", stock.getPrice()));
            // Append '%' onto end of mprice change percent. First '%' is escape char for '%'.
            mchangePercentTextView.setText(String.format(Locale.US, "%.2f%%", stock.getChangePercent()));

            // Assign green or red color to mprice change percent text
            if (stock.getChangePercent() < 0) {
                mchangePercentTextView.setTextColor(Color.RED);
            } else {
                mchangePercentTextView.setTextColor(Color.GREEN);
            }

            itemView.setOnClickListener(l -> listener.onItemClick(stock));
        }
    }

    private final BasicStockList mstocks;
    private final OnItemClickListener monItemClickListener;
    private boolean isDragging;

    public RecyclerAdapter(final BasicStockList stocks, final OnItemClickListener listener) {
        mstocks = stocks;
        monItemClickListener = listener;
        isDragging = false;
    }

    public void remove(final int position) {
        mstocks.remove(position);
        notifyItemRemoved(position);
    }

    public void swap(final int firstPosition, final int secondPosition) {
        Collections.swap(mstocks, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    @NonNull
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        final View stockView = inflater.inflate(R.layout.recycler_item_home, parent, false);

        return new ViewHolder(stockView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        holder.bind(mstocks.get(position), monItemClickListener);
    }

    @Override
    public int getItemCount() {
        return mstocks.size();
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setDragging(boolean dragging) {
        isDragging = dragging;
    }

}