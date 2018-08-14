package c.chasesriprajittichai.stockwatch.recyclerview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import java.util.Map;

import c.chasesriprajittichai.stockwatch.HomeActivity;
import c.chasesriprajittichai.stockwatch.R;
import c.chasesriprajittichai.stockwatch.stocks.BasicStockList;


public final class StockSwipeAndDragCallback extends ItemTouchHelper.SimpleCallback {

    private final HomeActivity homeActivity;
    private final RecyclerAdapter recyclerAdapter;
    private final BasicStockList stocks;
    private final Map<String, Integer> tickerToIndexMap;

    // Minimize the amount of allocation done in drawing methods
    private final Drawable whiteMargin;
    private final Drawable redBackground;
    private final Drawable garbageIcon;
    private final int garbageMargin;
    private final int whiteMarginSize = 4;

    public StockSwipeAndDragCallback(final HomeActivity homeActivity,
                                     final RecyclerAdapter recyclerAdapter,
                                     final BasicStockList stocks,
                                     final Map<String, Integer> tickerToIndexMap) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.DOWN |
                ItemTouchHelper.UP);
        this.homeActivity = homeActivity;
        this.recyclerAdapter = recyclerAdapter;
        this.stocks = stocks;
        this.tickerToIndexMap = tickerToIndexMap;

        whiteMargin = new ColorDrawable(Color.WHITE);
        redBackground = new ColorDrawable(Color.RED);
        garbageIcon = ContextCompat.getDrawable(homeActivity, R.drawable.ic_delete_black_24dp);
        final float dpUnit = homeActivity.getResources().getDisplayMetrics().density;
        garbageMargin = (int) (16 * dpUnit);
    }

    /**
     * Called when a viewHolder is swiped iv_left.
     *
     * @param viewHolder The ViewHolder which has been swiped by the user
     * @param direction  The direction to which the ViewHolder is swiped.
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
     * <p>
     * If this method returns true, ItemTouchHelper assumes {@code viewHolder}
     * has been moved to the adapter position of {@code target} ViewHolder.
     *
     * @param rv         The RecyclerView to which ItemTouchHelper is attached
     *                   to
     * @param viewHolder The ViewHolder which is being dragged by the user
     * @param target     The ViewHolder over which the currently active item is
     *                   being dragged
     * @return True if the {@code viewHolder} has been moved to the adapter
     * position of {@code target}
     */
    @Override
    public boolean onMove(final RecyclerView rv, final RecyclerView.ViewHolder viewHolder,
                          final RecyclerView.ViewHolder target) {
        return true;
    }

    /**
     * Called by ItemTouchHelper on RecyclerView's onDraw callback. This method
     * is called constantly while the user is swiping or dragging. This function
     * is where drawing "behind" the selected RecyclerView cell occurs.
     * <p>
     * The red background and garbage icon when swiping a stock iv_left are
     * drawn in this function.
     *
     * @param c                 The canvas which RecyclerView is drawing its
     *                          children
     * @param rv                The RecyclerView to which ItemTouchHelper is
     *                          attached to
     * @param viewHolder        The ViewHolder which is being interacted by the
     *                          User or it was interacted and simply animating
     *                          to its original position
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

        switch (actionState) {
            case ItemTouchHelper.ACTION_STATE_SWIPE:
                // Draw red background
                redBackground.setBounds(itemView.getRight() + (int) dX,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
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
                break;
        }

        super.onChildDraw(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    /**
     * Called by ItemTouchHelper on RecyclerView's onDraw callback. This method
     * is called constantly while the user is swiping or dragging. This function
     * is where drawing on the selected RecyclerView cell (includes drawing over
     * other cells) occurs.
     * <p>
     * The white margins around a selected cell that is being dragged are drawn
     * here. The actual cell switching occurs in this function as well. A more
     * traditional drag and drop RecyclerView implementation would execute cell
     * switches in {@link #onMove(RecyclerView, RecyclerView.ViewHolder,
     * RecyclerView.ViewHolder)}. But doing that restricts the precision about
     * when two cells swap. This function swaps cells when the selected cell is
     * more than 50% into the adjacent cell that it is invading. This function
     * takes care of the duties of {@link #onMove(RecyclerView,
     * RecyclerView.ViewHolder, RecyclerView.ViewHolder)}, causing {@link
     * #onMove(RecyclerView, RecyclerView.ViewHolder, RecyclerView.ViewHolder)}
     * to always return true. This function also notifies {@code
     * recyclerAdapter} if cells are being dragged, by setting {@link
     * RecyclerAdapter#setDragging(boolean)} to true.
     *
     * @param c                 The canvas which RecyclerView is drawing its
     *                          children
     * @param rv                The RecyclerView to which ItemTouchHelper is
     *                          attached to
     * @param viewHolder        The ViewHolder which is being interacted by the
     *                          User or it was
     *                          interacted and simply animating to its original
     *                          position
     * @param dX                The amount of horizontal displacement caused by
     *                          user's action
     * @param dY                The amount of vertical displacement caused by
     *                          user's action
     * @param actionState       The type of interaction on the View. Is either
     *                          {@code ACTION_STATE_DRAG} or {@code
     *                          ACTION_STATE_SWIPE}.
     * @param isCurrentlyActive True if this view is currently being controlled
     *                          by the user or false it is simply animating back
     *                          to its original state.
     */
    @Override
    public void onChildDrawOver(final Canvas c, final RecyclerView rv,
                                final RecyclerView.ViewHolder viewHolder,
                                final float dX, final float dY,
                                final int actionState, final boolean isCurrentlyActive) {
        final View itemView = viewHolder.itemView;

        /* This method can be called on ViewHolders that have already been
         * swiped away. Ignore these. */
        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        switch (actionState) {
            case ItemTouchHelper.ACTION_STATE_DRAG:
                if (isCurrentlyActive) {
                    /* This function is called multiple times after clearView()
                     * is called. Therefore, any calls to
                     * recyclerAdapter.setDragging() in clearView() could be
                     * erased by subsequent calls to this function. clearView()
                     * will be called after this method's isCurrentlyActive
                     * boolean parameter is set to false. */
                    recyclerAdapter.setDragging(true);

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

                    if (Math.abs(dY) > itemView.getHeight() / 2) {
                        /* If the selected cell is more than 50% invaded into
                         * an adjacent cell */

                        /* These calls replace what would normally be done in
                         * onMove(). For this reason, override onMove() to
                         * always return true. */
                        if (dY > 0) {
                            // Invading adjacent cell below
                            if (viewHolder.getAdapterPosition() < recyclerAdapter.getItemCount() - 1) {
                                recyclerAdapter.swap(viewHolder.getAdapterPosition(),
                                        viewHolder.getAdapterPosition() + 1);
                                homeActivity.updateTickerToIndexMap();
                            }
                        } else {
                            // Invading adjacent cell above
                            if (viewHolder.getAdapterPosition() > 0) {
                                recyclerAdapter.swap(viewHolder.getAdapterPosition(),
                                        viewHolder.getAdapterPosition() - 1);
                                homeActivity.updateTickerToIndexMap();
                            }
                        }
                    }
                }
                break;
        }

        super.onChildDrawOver(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    /**
     * Called by the ItemTouchHelper when the user interaction with an element
     * is over and it also completed its animation.
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
     */
    @Override
    public void clearView(final RecyclerView rv, final RecyclerView.ViewHolder viewHolder) {
        super.clearView(rv, viewHolder);
        recyclerAdapter.setDragging(false);
    }

    @Override
    public int getMovementFlags(final RecyclerView rv, final RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = ItemTouchHelper.LEFT;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public int getSwipeDirs(final RecyclerView rv, final RecyclerView.ViewHolder viewHolder) {
        return super.getSwipeDirs(rv, viewHolder);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

}
