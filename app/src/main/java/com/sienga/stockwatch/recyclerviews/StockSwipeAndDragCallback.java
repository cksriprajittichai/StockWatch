package com.sienga.stockwatch.recyclerviews;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import java.util.Map;

import com.sienga.stockwatch.HomeActivity;
import com.sienga.stockwatch.R;
import com.sienga.stockwatch.stocks.ConcreteStockWithEhValsList;


/**
 * Useful repo with explanations: https://github.com/iPaulPro/Android-ItemTouchHelper-Demo.
 */
public final class StockSwipeAndDragCallback extends ItemTouchHelper.SimpleCallback {

    private final HomeActivity homeActivity;
    private final StockRecyclerAdapter recyclerAdapter;
    private final ConcreteStockWithEhValsList stocks;
    private final Map<String, Integer> tickerToIndexMap;

    // Minimize the amount of allocation done in drawing methods
    private final Drawable whiteMargin;
    private final Drawable redBackground;
    private final Drawable darkGrayBackground;
    private final Drawable garbageIcon;
    private final int garbageMargin;
    private final int whiteMarginSize = 4;

    public StockSwipeAndDragCallback(final HomeActivity homeActivity,
                                     final StockRecyclerAdapter recyclerAdapter,
                                     final ConcreteStockWithEhValsList stocks,
                                     final Map<String, Integer> tickerToIndexMap) {
        super(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.DOWN | ItemTouchHelper.UP);
        this.homeActivity = homeActivity;
        this.recyclerAdapter = recyclerAdapter;
        this.stocks = stocks;
        this.tickerToIndexMap = tickerToIndexMap;

        whiteMargin = new ColorDrawable(Color.WHITE);
        redBackground = new ColorDrawable(Color.RED);
        darkGrayBackground = new ColorDrawable(Color.DKGRAY);
        garbageIcon = ContextCompat.getDrawable(homeActivity, R.drawable.ic_delete_black_24dp);
        final float dpUnit = homeActivity.getResources().getDisplayMetrics().density;
        garbageMargin = (int) (16 * dpUnit);
    }

    /**
     * Called when a viewHolder is swiped left.
     *
     * @param viewHolder The ArticleViewHolder which has been swiped by the user
     * @param direction  The direction to which the ArticleViewHolder is swiped.
     *                   Should always be {@link ItemTouchHelper#LEFT}.
     */
    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, final int direction) {
        final int position = viewHolder.getAdapterPosition();
        tickerToIndexMap.remove(stocks.get(position).getTicker()); // Do before removal from stocks
        recyclerAdapter.remove(position); // Removes from rview and from stocks, and updates rview
        homeActivity.updateTickerToIndexMap(position);
    }

    /**
     * Called when ItemTouchHelper wants to move the dragged item from its old
     * position to the new position.
     *
     * @param rv         The RecyclerView to which ItemTouchHelper is attached
     *                   to
     * @param viewHolder The ArticleViewHolder which is being dragged by the
     *                   user
     * @param target     The ArticleViewHolder over which the currently active
     *                   item is being dragged
     * @return True because viewHolder has been moved to the adapter
     * position of target
     */
    @Override
    public boolean onMove(final RecyclerView rv, final RecyclerView.ViewHolder viewHolder,
                          final RecyclerView.ViewHolder target) {
        recyclerAdapter.swap(viewHolder.getAdapterPosition(),
                target.getAdapterPosition());
        homeActivity.updateTickerToIndexMap();

        homeActivity.notifyRvSortInvalidated();
        return true;
    }

    /**
     * Called by ItemTouchHelper on RecyclerView's onDraw callback. This method
     * is called repeatedly while the user is swiping or dragging. This method
     * is where drawing "behind" the selected RecyclerView cell occurs.
     * <p>
     * If the user is swiping, {@link #redBackground} and {@link #garbageIcon}
     * are drawn. If the user is dragging, {@link #darkGrayBackground} is
     * drawn.
     *
     * @param c                 The canvas which RecyclerView is drawing its
     *                          children
     * @param rv                The RecyclerView to which ItemTouchHelper is
     *                          attached to
     * @param viewHolder        The ArticleViewHolder which is being interacted
     *                          by the user or it was interacted and simply
     *                          animating to its original position
     * @param dX                The amount of horizontal displacement caused by
     *                          user's action
     * @param dY                The amount of vertical displacement caused by
     *                          user's action
     * @param actionState       The type of interaction on the View. Is either
     *                          {@code ACTION_STATE_DRAG} or
     *                          {@code ACTION_STATE_SWIPE}.
     * @param isCurrentlyActive True if this view is currently being controlled
     *                          by the user or false it is simply animating back
     *                          to its original state
     */
    @Override
    public void onChildDraw(final Canvas c, final RecyclerView rv,
                            final RecyclerView.ViewHolder viewHolder,
                            final float dX, final float dY,
                            final int actionState, final boolean isCurrentlyActive) {
        final View itemView = viewHolder.itemView;

        /* This method can be called on ViewHolders that have already been
         * swiped away. Ignore these. */
        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Draw red background
            redBackground.setBounds(
                    itemView.getRight() + (int) dX,
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom());
            redBackground.draw(c);

            // Draw garbage can
            final int itemHeight = itemView.getBottom() - itemView.getTop();
            final int intrinsicWidth = garbageIcon.getIntrinsicWidth();
            final int intrinsicHeight = garbageIcon.getIntrinsicHeight();

            final int xMarkLeft = itemView.getRight() - garbageMargin - intrinsicWidth;
            final int xMarkRight = itemView.getRight() - garbageMargin;
            final int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            final int xMarkBottom = xMarkTop + intrinsicHeight;
            garbageIcon.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
            garbageIcon.draw(c);
        } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // Draw black background
            darkGrayBackground.setBounds(
                    itemView.getLeft(),
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom());
            darkGrayBackground.draw(c);
        }

        super.onChildDraw(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }


    /**
     * Called by ItemTouchHelper on RecyclerView's onDraw callback. This method
     * is called repeatedly while the user is swiping or dragging. This method
     * is where drawing on the selected RecyclerView cell (includes drawing over
     * other cells) occurs.
     * <p>
     * The white margins around a selected cell that is being dragged are drawn
     * in this method.
     *
     * @param c                 The canvas which RecyclerView is drawing its
     *                          children
     * @param rv                The RecyclerView to which ItemTouchHelper is
     *                          attached to
     * @param viewHolder        The ArticleViewHolder which is being interacted
     *                          by the user or it was interacted and simply
     *                          animating to its original position
     * @param dX                The amount of horizontal displacement caused by
     *                          user's action
     * @param dY                The amount of vertical displacement caused by
     *                          user's action
     * @param actionState       The type of interaction on the View. Is either
     *                          {@code ACTION_STATE_DRAG} or {@code
     *                          ACTION_STATE_SWIPE}.
     * @param isCurrentlyActive True if this view is currently being controlled
     *                          by the user or false it is simply animating back
     *                          to its original state
     */
    @Override
    public void onChildDrawOver(final Canvas c, final RecyclerView rv,
                                final RecyclerView.ViewHolder viewHolder,
                                final float dX, float dY,
                                final int actionState, final boolean isCurrentlyActive) {
        final View itemView = viewHolder.itemView;

        /* This method can be called on ViewHolders that have already been
         * swiped away. Ignore these. */
        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        if (isCurrentlyActive && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // y increases as we move further down the screen
            // Draw left margin
            whiteMargin.setBounds(
                    itemView.getLeft(),
                    itemView.getTop() + (int) dY,
                    itemView.getLeft() + whiteMarginSize,
                    itemView.getBottom() + (int) dY);
            whiteMargin.draw(c);

            // Draw right margin
            whiteMargin.setBounds(
                    itemView.getRight() - whiteMarginSize,
                    itemView.getTop() + (int) dY,
                    itemView.getRight(),
                    itemView.getBottom() + (int) dY);
            whiteMargin.draw(c);

            // Draw top margin
            whiteMargin.setBounds(
                    itemView.getLeft(),
                    itemView.getTop() + (int) dY,
                    itemView.getRight(),
                    itemView.getTop() + (int) dY + whiteMarginSize);
            whiteMargin.draw(c);

            // Draw bottom margin
            whiteMargin.setBounds(
                    itemView.getLeft(),
                    itemView.getBottom() + (int) dY - whiteMarginSize,
                    itemView.getRight(),
                    itemView.getBottom() + (int) dY);
            whiteMargin.draw(c);
        }

        super.onChildDrawOver(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    /**
     * Called when the ViewHolder swiped or dragged by the ItemTouchHelper is
     * changed. If the user is swiping or dragging a RecyclerView cell, notify
     * {@link #recyclerAdapter}.
     *
     * @param viewHolder  The new ViewHolder that is being swiped or dragged.
     *                    Might be null if it is cleared.
     * @param actionState One of {@link ItemTouchHelper#ACTION_STATE_IDLE},
     *                    {@link ItemTouchHelper#ACTION_STATE_SWIPE} or
     *                    {@link ItemTouchHelper#ACTION_STATE_DRAG}
     * @see #clearView(RecyclerView, RecyclerView.ViewHolder)
     */
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG ||
                actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            recyclerAdapter.setSwipingOrDragging(true);
        }
    }

    /**
     * Called by the ItemTouchHelper when the user interaction with an element
     * is over and it also completed its animation. Ensure that {@link
     * #recyclerAdapter} knows that the user is not swiping or dragging.
     * <p>
     * This is a good place to clear all changes on the View that was done in
     * {@link #onSelectedChanged(RecyclerView.ViewHolder, int)}, {@link
     * #onChildDraw(Canvas, RecyclerView, RecyclerView.ViewHolder, float, float,
     * int, boolean)} or {@link #onChildDrawOver(Canvas, RecyclerView,
     * RecyclerView.ViewHolder, float, float, int, boolean)}.
     *
     * @param rv         The RecyclerView which is controlled by the
     *                   ItemTouchHelper
     * @param viewHolder The View that was interacted by the user
     * @see #onSelectedChanged(RecyclerView.ViewHolder, int)
     */
    @Override
    public void clearView(final RecyclerView rv, final RecyclerView.ViewHolder viewHolder) {
        super.clearView(rv, viewHolder);
        recyclerAdapter.setSwipingOrDragging(false);
    }

    @Override
    public int getMovementFlags(final RecyclerView rv, final RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = ItemTouchHelper.LEFT;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    /**
     * Returns whether ItemTouchHelper should start a drag and drop operation if
     * an item is long pressed.
     * <p>
     * Default value returns true but you may want to disable this if you want
     * to start dragging on a custom view touch using {@link
     * ItemTouchHelper#startDrag(RecyclerView.ViewHolder)}.
     *
     * @return True if ItemTouchHelper should start dragging an item when it is
     * long pressed, false otherwise. Default value is true.
     * @see ItemTouchHelper#startDrag(RecyclerView.ViewHolder)
     */
    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    /**
     * Returns whether ItemTouchHelper should start a swipe operation if a
     * pointer is swiped over the View.
     * <p>
     * Default value returns true but you may want to disable this if you want
     * to start swiping on a custom view touch using {@link
     * ItemTouchHelper#startSwipe(RecyclerView.ViewHolder)}.
     *
     * @return True if ItemTouchHelper should start swiping an item when user
     * swipes a pointer over the View, false otherwise. Default value is true.
     * @see ItemTouchHelper#startSwipe(RecyclerView.ViewHolder)}.
     */
    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

}
