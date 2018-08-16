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
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithAhVals;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithAhValsList;
import c.chasesriprajittichai.stockwatch.stocks.Stock;


public final class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {


    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView state;
        private final TextView ticker;
        private final TextView price;
        private final TextView changePercent;

        ViewHolder(final View v) {
            super(v);

            state = v.findViewById(R.id.textView_state_homeRecyclerItem);
            ticker = v.findViewById(R.id.textView_ticker_homeRecyclerItem);
            price = v.findViewById(R.id.textView_price_homeRecyclerItem);
            changePercent = v.findViewById(R.id.textView_changePercent_homeRecyclerItem);
        }

        void bind(final ConcreteStockWithAhVals stock, final OnItemClickListener listener) {
            if (stock.getState() == Stock.State.AFTER_HOURS) {
                // After hours state is the only state with an unwanted character in the enum name
                state.setText(String.format(Locale.US, "%s", "AFTER HOURS"));
            } else {
                state.setText(stock.getState().toString());
            }
            ticker.setText(stock.getTicker());
            price.setText(String.format(Locale.US, "%.2f", stock.getLivePrice()));

            if (stock.getNetChangePercent() < 0) {
                // '-' is already part of the number
                changePercent.setText(String.format(Locale.US,
                        "%.2f%%", stock.getNetChangePercent()));
                changePercent.setTextColor(Color.RED);
            } else {
                changePercent.setText(String.format(Locale.US,
                        "+%.2f%%", stock.getNetChangePercent()));
                changePercent.setTextColor(Color.GREEN);
            }

            itemView.setOnClickListener(l -> listener.onItemClick(stock));
        }

    }


    private final ConcreteStockWithAhValsList stocks;
    private final OnItemClickListener onItemClickListener;
    private boolean isDragging;

    public RecyclerAdapter(final ConcreteStockWithAhValsList stocks, final OnItemClickListener listener) {
        this.stocks = stocks;
        onItemClickListener = listener;
        isDragging = false;
    }

    public void remove(final int position) {
        stocks.remove(position);
        notifyItemRemoved(position);
    }

    public void swap(final int firstPosition, final int secondPosition) {
        Collections.swap(stocks, firstPosition, secondPosition);
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
        holder.bind(stocks.get(position), onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setDragging(boolean dragging) {
        isDragging = dragging;
    }


    public interface OnItemClickListener {

        void onItemClick(final ConcreteStockWithAhVals stock);

    }

}