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

public class StockSwipeLeftCallback extends ItemTouchHelper.SimpleCallback {

    private final StockSwipeLeftListener mswipeLeftListener;

    // Minimize the amount of object allocation done in onChildDraw()
    private final Drawable background;
    private final Drawable garbageIcon;
    private final int garbageMargin;

    StockSwipeLeftCallback(Context context, StockSwipeLeftListener swipeLeftListener) {
        super(0, ItemTouchHelper.LEFT);

        mswipeLeftListener = swipeLeftListener;

        background = new ColorDrawable(Color.RED);
        garbageIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp);
        final float dpUnit = context.getResources().getDisplayMetrics().density;
        garbageMargin = (int) (16 * dpUnit);
    }


    @Override
    public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public int getSwipeDirs(RecyclerView rv, RecyclerView.ViewHolder viewHolder) {
        return super.getSwipeDirs(rv, viewHolder);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        final int position = viewHolder.getAdapterPosition();
        mswipeLeftListener.onStockSwipedLeft(position);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView rv, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        /* This method can be called on ViewHolders that have already been swiped away.
         * Ignore these. */
        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        // Draw red background
        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        background.draw(c);

        // Draw garbage can
        int itemHeight = itemView.getBottom() - itemView.getTop();
        int intrinsicWidth = garbageIcon.getIntrinsicWidth();
        int intrinsicHeight = garbageIcon.getIntrinsicHeight();

        int xMarkLeft = itemView.getRight() - garbageMargin - intrinsicWidth;
        int xMarkRight = itemView.getRight() - garbageMargin;
        int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
        int xMarkBottom = xMarkTop + intrinsicHeight;
        garbageIcon.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

        garbageIcon.draw(c);

        super.onChildDraw(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
