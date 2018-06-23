package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import c.chasesriprajittichai.stockwatch.listeners.StockSwipeLeftListener;

public final class StockSwipeLeftCallback extends ItemTouchHelper.SimpleCallback {

    private final StockSwipeLeftListener mswipeLeftListener;

    // Minimize the amount of object allocation done in onChildDraw()
    private final Drawable background;
    private final Drawable garbageIcon;
    private final int garbageMargin;

    StockSwipeLeftCallback(final Context context, final StockSwipeLeftListener swipeLeftListener) {
        super(0, ItemTouchHelper.LEFT);
        mswipeLeftListener = swipeLeftListener;

        background = new ColorDrawable(Color.RED);
        garbageIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp);
        final float dpUnit = context.getResources().getDisplayMetrics().density;
        garbageMargin = (int) (16 * dpUnit);
    }


    @Override
    public boolean onMove(final RecyclerView rv, final RecyclerView.ViewHolder viewHolder, final RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public int getSwipeDirs(final RecyclerView rv, final RecyclerView.ViewHolder viewHolder) {
        return super.getSwipeDirs(rv, viewHolder);
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, final int direction) {
        final int position = viewHolder.getAdapterPosition();
        mswipeLeftListener.onStockSwipedLeft(position);
    }

    @Override
    public void onChildDraw(final Canvas c, final RecyclerView rv, final RecyclerView.ViewHolder viewHolder,
                            final float dX, final float dY, final int actionState, final boolean isCurrentlyActive) {
        final View itemView = viewHolder.itemView;

        /* This method can be called on ViewHolders that have already been swiped away.
         * Ignore these. */
        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        // Draw red background
        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        background.draw(c);

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

        super.onChildDraw(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
