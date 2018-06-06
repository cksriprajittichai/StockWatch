package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerHomeDivider extends RecyclerView.ItemDecoration {

    private Drawable mdivider;

    public RecyclerHomeDivider(Context context) {
        mdivider = ContextCompat.getDrawable(context, R.drawable.recycler_view_divider_home);
    }
    
    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mdivider.getIntrinsicHeight();

            mdivider.setBounds(left, top, right, bottom);
            mdivider.draw(c);
        }
    }
}
