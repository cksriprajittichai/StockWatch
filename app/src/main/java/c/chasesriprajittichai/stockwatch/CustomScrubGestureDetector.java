package c.chasesriprajittichai.stockwatch;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import java.util.Collections;
import java.util.List;

/**
 * This class is modeled after com.robinhood.spark.ScrubGestureDetector.
 * <p>
 * In order to dynamically display the time at the current scrubbing position while scrubbing, we
 * need to send the index of the scrubbing line to the IndividualStockActivity. Given the index of
 * the scrubbing line, the IndividualStockActivity can determine the time at that moment because the
 * time scale of each chart period is a constant and known period, which therefore has a constant
 * and known step-size. This class and CustomSparkView have been created in order to do this.
 * <p>
 * The encapsulation of com.robinhood.spark makes it difficult to get the index of the scrubbing
 * line to an IndividualStockActivity. CustomScrubGestureDetector extends View.OnTouchListener and
 * should be set as the View.onTouchListener of the SparkView being used in IndividualStockActivity,
 * instead of com.robinhood.spark.ScrubGestureDetector. This class copies the behavior of
 * com.robinhood.spark.ScrubGestureDetector. The main difference between them is that the
 * CustomScrubGestureDetector defines a ScrubIndexListener interface that provides callback methods
 * that provide the index of the scrubbing line as a parameter. The IndividualStockActivity should
 * implement the ScrubIndexListener interface in order to receive callbacks about the index
 * of the scrubbing line.
 * <p>
 * The com.robinhood.spark.ScrubGestureDetector is only used by com.robinhood.SparkView in the
 * following ways: 1) in com.robinhood.spark.SparkView.setScrubEnabled() the ScrubGestureDetector is
 * enabled/disabled; 2) com.robinhood.spark.SparkView implements
 * com.robinhood.spark.ScrubGestureDetector.ScrubListener - the methods declared in the interface
 * are onScrubbed(float x, float y), and onScrubEnded(). When creating CustomScrubGestureDetector,
 * we know that we must consider these two usages in order to keep the SparkView's scrubbing feature
 * working. We are not using the com.robinhood.spark.ScrubGestureDetector's enabled boolean, so it
 * has been removed (always true) from this class. com.robinhood.spark.ScrubGestureDetector has a
 * member variable of type com.robinhood.spark.ScrubGestureDetector.ScrubListener that it makes
 * function calls to in various functions defined in the class. com.robinhood.spark.ScrubGestureDetector
 * and com.robinhood.spark.ScrubGestureDetector.ScrubListener are package private, meaning that
 * CustomScrubGestureDetector cannot implement/extend either interface/class. Instead,
 * CustomScrubGestureDetector defines the CustomScrubGestureDetector.ScrubListener interface which
 * declare onScrubbed(final float x, final float y), and onScrubEnded(). These two function
 * signatures are the same as the function signatures of the two functions declared by
 * com.robinhood.spark.ScrubGestureDetector.ScrubListener. CustomSparkView implements
 * CustomScrubGestureDetector. The definitions of onScrubbed(float x, float y) and onScrubEnded()
 * in com.robinhood.spark.SparkView do not use either CustomScrubGestureDetector or
 * com.robinhood.spark.ScrubGestureDetector. Therefore, there is no need for CustomSparkView to
 * override onScrubbed(float x, float y) or onScrubEnded(), because the parent definitions from
 * com.robinhood.spark.SparkView are sufficient.
 */
public final class CustomScrubGestureDetector implements View.OnTouchListener {

    private static final long LONG_PRESS_TIMEOUT_MS = 250;

    private final CustomSparkView msparkView;
    private final ScrubListener mscrubListener;
    private final ScrubIndexListener mscrubIndexListener;
    private final Handler mhandler;
    private final float mtouchSlop;
    private float downX, downY;

    private final Runnable mlongPressRunnable = new Runnable() {
        @Override
        public void run() {
            mscrubListener.onScrubbed(downX, downY);
            mscrubIndexListener.onScrubbed(getNearestIndex(downX));
        }
    };

    public CustomScrubGestureDetector(final CustomSparkView customSparkView,
                                      final CustomScrubGestureDetector.ScrubIndexListener scrubIndexListener,
                                      final float touchSlop) {
        msparkView = customSparkView;
        mscrubListener = customSparkView;
        mscrubIndexListener = scrubIndexListener;
        mtouchSlop = touchSlop;
        mhandler = new Handler();
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Store the time to compute whether future events are 'long presses'
                downX = x;
                downY = y;

                mhandler.postDelayed(mlongPressRunnable, LONG_PRESS_TIMEOUT_MS);
                return true;
            case MotionEvent.ACTION_MOVE:
                // Calculate the elapsed time since the down event
                final float timeDelta = event.getEventTime() - event.getDownTime();

                // If the user has intentionally long-pressed
                if (timeDelta >= LONG_PRESS_TIMEOUT_MS) {
                    mhandler.removeCallbacks(mlongPressRunnable);
                    mscrubListener.onScrubbed(x, y);
                    mscrubIndexListener.onScrubbed(getNearestIndex(x));
                } else {
                    // If we moved before longpress, remove the callback if we exceeded the tap slop
                    float deltaX = x - downX;
                    float deltaY = y - downY;
                    if (deltaX >= mtouchSlop || deltaY >= mtouchSlop) {
                        /* We got a MOVE event that exceeded tap slop but before the long-press
                         * threshold, we don't care about this series of events anymore. */
                        mhandler.removeCallbacks(mlongPressRunnable);
                        return false;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mhandler.removeCallbacks(mlongPressRunnable);
                mscrubListener.onScrubEnded();
                mscrubIndexListener.onScrubEnded();
                return true;
            default:
                return false;
        }
    }

    private int getNearestIndex(final float x) {
        final List<Float> points = msparkView.getXPoints();

        int index = Collections.binarySearch(points, x);

        // If binary search returns positive, it is an exact match. Return this index.
        if (index >= 0) return index;

        // Otherwise, calculate the binary search's specified insertion index
        index = -1 - index;

        // If inserting at 0, then our guaranteed nearest index is 0
        if (index == 0) return index;

        // If inserting at the very end, then our guaranteed nearest index is the final one
        if (index == points.size()) return --index;

        // Otherwise we need to check which of our two neighbors we're closer to
        final double deltaUp = points.get(index) - x;
        final double deltaDown = x - points.get(index - 1);
        if (deltaUp > deltaDown) {
            // If the below neighbor is closer, decrement index
            index--;
        }

        return index;
    }

    public interface ScrubListener {
        void onScrubbed(final float x, final float y);

        void onScrubEnded();
    }

    public interface ScrubIndexListener {
        // Called at same time as ScrubListener.onScrubbed().
        void onScrubbed(final int index);

        // Called at same time as ScrubListener.onScrubEnded().
        void onScrubEnded();
    }

}
