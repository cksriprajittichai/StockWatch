package c.chasesriprajittichai.stockwatch.recyclerviews;

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

import c.chasesriprajittichai.stockwatch.HomeActivity;
import c.chasesriprajittichai.stockwatch.IndividualStockActivity;
import c.chasesriprajittichai.stockwatch.R;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithAhVals;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithAhValsList;
import c.chasesriprajittichai.stockwatch.stocks.Stock;


public final class StockRecyclerAdapter extends RecyclerView.Adapter<StockRecyclerAdapter.ViewHolder> {


    /**
     * A {@link RecyclerView.ViewHolder} that will represent the information of
     * a {@link ConcreteStockWithAhVals}.
     * <p>
     * To understand why only ConcreteStockWithAhVals are shown, look at {@link
     * ConcreteStockWithAhVals}.
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView state;
        private final TextView ticker;
        private final TextView price;
        private final TextView changePercent;

        ViewHolder(final View v) {
            super(v);

            state = v.findViewById(R.id.textView_state_stockRecyclerItem);
            ticker = v.findViewById(R.id.textView_ticker_stockRecyclerItem);
            price = v.findViewById(R.id.textView_price_stockRecyclerItem);
            changePercent = v.findViewById(R.id.textView_changePercent_stockRecyclerItem);
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


    /**
     * The list of {@link ConcreteStockWithAhVals} to be shown in {@link
     * HomeActivity#rv}
     * <p>
     * To understand why only ConcreteStockWithAhVals are shown, look at {@link
     * ConcreteStockWithAhVals}.
     */
    private final ConcreteStockWithAhValsList stocks;

    private final OnItemClickListener onItemClickListener;

    /**
     * Used so that {@link HomeActivity} can know whether or not a cell in
     * {@link HomeActivity#rv} is being dragged. This is useful for when
     * HomeActivity is trying to update the rv. If rv is updated while a cell is
     * dragging, the dragging will be stopped (as if the user lifted their
     * finger off the screen). HomeActivity avoids this by checking if a cell is
     * dragging before updating rv.
     */
    private boolean isDragging;

    public StockRecyclerAdapter(final ConcreteStockWithAhValsList stocks, final OnItemClickListener listener) {
        this.stocks = stocks;
        onItemClickListener = listener;
        isDragging = false;
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
     * This new ViewHolder should be constructed with a new View that can
     * represent the items of the given type. You can either create a new View
     * manually or inflate it from an XML layout file.
     * <p>
     * The new ViewHolder will be used to display items of the adapter using
     * {@link #onBindViewHolder(RecyclerView.ViewHolder, int, List)}.
     *
     * @param parent   The ViewGroup into which the new View will be added after
     *                 it is bound to an adapter position
     * @param viewType The view type of the new View
     * @return A new ViewHolder that holds a View of the given view type
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    @NonNull
    @Override
    public StockRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        final View stockView = inflater.inflate(R.layout.recycler_item_stock_stock_recycler, parent, false);

        return new ViewHolder(stockView);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method should update the contents of the {@link
     * RecyclerView.ViewHolder#itemView} to reflect the item at the given
     * position.
     *
     * @param holder   The ViewHolder which should be updated to represent the
     *                 contents of the item at the given position in the data set
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
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
     * @return Value of isDragging
     * @see #isDragging
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * @param dragging Value to set isDragging to
     * @see #isDragging
     */
    public void setDragging(boolean dragging) {
        isDragging = dragging;
    }


    public interface OnItemClickListener {

        void onItemClick(final ConcreteStockWithAhVals stock);

    }

}