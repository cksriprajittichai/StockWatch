package com.sienga.stockwatch.recyclerviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.sienga.stockwatch.R;


public final class NewsRecyclerDivider extends RecyclerView.ItemDecoration {

    private final Drawable divider;

    public NewsRecyclerDivider(final Context context) {
        divider = ContextCompat.getDrawable(context, R.drawable.recycler_divider_news_individual);
    }

    /**
     * Draw any appropriate decorations into the Canvas supplied to the
     * RecyclerView. Any content drawn by this method will be drawn after the
     * item views are drawn and will thus appear over the views.
     *
     * @param c      Canvas to draw into
     * @param parent RecyclerView this ItemDecoration is drawing into
     * @param state  The current state of RecyclerView
     */
    @Override
    public void onDrawOver(final Canvas c, final RecyclerView parent, final RecyclerView.State state) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();

        final int childCount = parent.getChildCount();
        View child;
        RecyclerView.LayoutParams params;
        int top, bottom, i;
        for (i = 0; i < childCount; i++) {
            child = parent.getChildAt(i);
            params = (RecyclerView.LayoutParams) child.getLayoutParams();

            top = child.getBottom() + params.bottomMargin;
            bottom = top + divider.getIntrinsicHeight();

            divider.setBounds(left, top, right, bottom);
            divider.draw(c);
        }
    }

}
