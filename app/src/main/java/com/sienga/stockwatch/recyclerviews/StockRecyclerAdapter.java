package com.sienga.stockwatch.recyclerviews;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.sienga.stockwatch.HomeActivity;
import com.sienga.stockwatch.R;
import com.sienga.stockwatch.stocks.ConcreteStockWithEhVals;
import com.sienga.stockwatch.stocks.ConcreteStockWithEhValsList;


public final class StockRecyclerAdapter extends RecyclerView.Adapter<StockRecyclerAdapter.StockViewHolder> {


    /**
     * A {@link RecyclerView.ViewHolder} that will represent the information of
     * a {@link ConcreteStockWithEhVals}.
     * <p>
     * To understand why only ConcreteStockWithEhVals are shown, look at {@link
     * ConcreteStockWithEhVals}.
     */
    class StockViewHolder extends RecyclerView.ViewHolder {

        private final TextView ticker;
        private final TextView name;
        private final TextView price;
        private final TextView changePercent;

        StockViewHolder(final View v) {
            super(v);

            ticker = v.findViewById(R.id.textView_ticker_stockRecyclerItem);
            name = v.findViewById(R.id.textView_name_stockRecyclerItem);
            price = v.findViewById(R.id.textView_price_stockRecyclerItem);
            changePercent = v.findViewById(R.id.textView_changePercent_stockRecyclerItem);
        }

        void bind(final ConcreteStockWithEhVals stock, final OnItemClickListener listener) {
            ticker.setText(stock.getTicker());
            name.setText(stock.getName());
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


    /**
     * The list of {@link ConcreteStockWithEhVals} to be shown in {@link
     * HomeActivity#rv}
     * <p>
     * To understand why only ConcreteStockWithEhVals are shown, look at {@link
     * ConcreteStockWithEhVals}.
     */
    private final ConcreteStockWithEhValsList stocks;

    private final OnItemClickListener onItemClickListener;

    /**
     * Used so that {@link HomeActivity} can know whether or not a cell in
     * {@link HomeActivity#rv} is being swiped or dragged. This is useful when
     * HomeActivity is trying to update the rv. If rv is updated while a cell is
     * swiping or dragging, the action will be stopped (as if the user lifted
     * their finger off the screen). HomeActivity avoids this by checking if a
     * cell is swiping or dragging before updating rv.
     *
     * @see HomeActivity#onResponse(ConcreteStockWithEhValsList)
     */
    private boolean isSwipingOrDragging;

    public StockRecyclerAdapter(final ConcreteStockWithEhValsList stocks, final OnItemClickListener listener) {
        this.stocks = stocks;
        onItemClickListener = listener;
        isSwipingOrDragging = false;
    }

    /**
     * Removes a stock from {@link #stocks} and calls {@link
     * #notifyItemRemoved(int)}. This method exists to allow for simpler removal
     * of stocks. Called only when a user swipe-deletes a stock. The only other
     * way to delete a stock is to open it in IndividualStockActivity, then
     * unstar. This process deletes the stock through removing it's information
     * from preferences.
     *
     * @param position Position of the stock to remove
     */
    public void remove(final int position) {
        stocks.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * Called from {@link StockSwipeAndDragCallback} when the user is "moving"
     * by long-pressing the stock, then dragging it up or down. Called when two
     * RecyclerView cells have switched positions.
     *
     * @param firstPosition  First swap position
     * @param secondPosition Second swap position
     */
    public void swap(final int firstPosition, final int secondPosition) {
        Collections.swap(stocks, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    /**
     * Called when RecyclerView needs a new {@link RecyclerView.ViewHolder} of
     * the given type to represent an item.
     * <p>
     * This new StockViewHolder should be constructed with a new View that can
     * represent the items of the given type. You can either create a new View
     * manually or inflate it from an XML layout file.
     * <p>
     * The new StockViewHolder will be used to display items of the adapter using
     * {@link #onBindViewHolder(RecyclerView.ViewHolder, int, List)}.
     *
     * @param parent   The ViewGroup into which the new View will be added after
     *                 it is bound to an adapter position
     * @param viewType The view type of the new View
     * @return A new StockViewHolder that holds a View of the given view type
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        final View stockView = inflater.inflate(R.layout.recycler_item_stock_stock_recycler, parent, false);

        return new StockViewHolder(stockView);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method should update the contents of the {@link
     * RecyclerView.ViewHolder#itemView} to reflect the item at the given
     * position.
     *
     * @param holder   The StockViewHolder which should be updated to represent the
     *                 contents of the item at the given position in the data set
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull final StockViewHolder holder, final int position) {
        holder.bind(stocks.get(position), onItemClickListener);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter
     */
    @Override
    public int getItemCount() {
        return stocks.size();
    }

    /**
     * @return Value of isSwipingOrDragging
     * @see #isSwipingOrDragging
     */
    public boolean isSwipingOrDragging() {
        return isSwipingOrDragging;
    }

    /**
     * @param swipingOrDragging Value to set isSwipingOrDragging to
     * @see #isSwipingOrDragging
     */
    public void setSwipingOrDragging(boolean swipingOrDragging) {
        isSwipingOrDragging = swipingOrDragging;
    }


    public interface OnItemClickListener {

        void onItemClick(final ConcreteStockWithEhVals stock);

    }

}