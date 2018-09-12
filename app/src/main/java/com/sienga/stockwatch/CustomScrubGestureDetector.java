/**
 * Copyright (C) 2016 Robinhood Markets, Inc.
 * Modifications Copyright (C) 2018, Chase Sriprajittichai
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sienga.stockwatch;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import com.robinhood.spark.SparkView;

import java.util.Collections;
import java.util.List;


/**
 * This class is modeled after {@link com.robinhood.spark.ScrubGestureDetector}.
 * <p>
 * While scrubbing, in order to dynamically display time values at the current
 * scrubbing position, we need to send the index of the scrubbing line to the
 * {@link IndividualStockActivity}. Given the index of the scrubbing line, the
 * IndividualStockActivity can determine the time at that moment because the
 * time scale of each chart period is a constant and known period, which
 * therefore has a constant and known step-size. This class and {@link
 * CustomSparkView} have been created in order to do this.
 * <p>
 * This class should be set as the OnTouchListener of the CustomSparkView being
 * used in IndividualStockActivity, instead of robinhood's ScrubGestureDetector.
 * The IndividualStockActivity should implement the ScrubIndexListener interface
 * in order to receive callbacks about the index of the scrubbing line.
 * <p>
 * This class mainly copies the behavior of robinhood's ScrubGestureDetector,
 * but contains callbacks to {@link ScrubIndexListener} whenever a callback to
 * {@link com.robinhood.spark.ScrubGestureDetector.ScrubListener} is made.
 * Through the ScrubIndexListener callbacks, the index of the scrubbing line can
 * be passed as a parameter to the listener. The callbacks contained in
 * inherited code to robinhood's ScrubListener work properly - the only reason
 * that {@link ScrubListener} exists - with identical methods defined as those
 * defined in robinhood's ScrubListener - is that CustomScrubGestureDetector
 * cannot reference robinhood's ScrubListener because of the way it is
 * encapsulated, and therefore cannot make the same ScrubListener callbacks that
 * ScrubGestureDetector makes. By defining our own ScrubListener, from this
 * class, we can declare (and make calls to) the methods that are defined
 * (declared in robinhood's ScrubListener, and our ScrubListener) in SparkView,
 * without ever overriding SparkView's definition of the methods.
 * <p>
 * ScrubGestureDetector is only used by SparkView in the following two ways:
 * <ul>
 * <li> in {@link SparkView#setScrubEnabled(boolean)}, the ScrubGestureDetector
 * is enabled/disabled
 * <li> SparkView implements robinhood's ScrubListener - the methods declared in
 * the interface are {@link SparkView#onScrubbed(float x, float y)}, and {@link
 * SparkView#onScrubEnded()}
 * </ul>
 * When creating this class, we know that we must account for these two usages
 * in order to keep the CustomSparkView's (which extends SparkView) scrubbing
 * feature working. We are not using ScrubGestureDetector's enabled (boolean)
 * member variable (always true for us), so it has been removed from this class.
 * ScrubGestureDetector has a reference to a robinhood ScrubListener that it
 * makes function calls to from multiple functions in the class.
 * ScrubGestureDetector and robinhood's ScrubListener are package private, which
 * means that CustomScrubGestureDetector cannot implement or extend either
 * interface or class. Instead, CustomScrubGestureDetector defines the
 * ScrubListener interface which declares {@link
 * ScrubListener#onScrubbed(float, float)} and {@link
 * ScrubListener#onScrubEnded()}. These two function signatures are the same as
 * the two function signatures declared by robinhood's ScrubListener. For our
 * purposes, a callback to a ScrubIndexListener should not occur within
 * onScrubbed(float x, float y) or onScrubEnded(), meaning that we do not need
 * to change (override) the inherited functionality from SparkView. Therefore,
 * CustomSparkView does not need to override these methods because its
 * inherited definitions from SparkView are sufficient.
 *
 * @see CustomSparkView
 */
public final class CustomScrubGestureDetector implements View.OnTouchListener {

    private static final long LONG_PRESS_TIMEOUT_MS = 250;

    private final CustomSparkView sparkView;
    private final ScrubListener scrubListener;
    private final ScrubIndexListener scrubIndexListener;
    private final Handler handler;
    private final float touchSlop;
    private float downX, downY;

    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            scrubListener.onScrubbed(downX, downY);
            scrubIndexListener.onScrubbed(getNearestIndex(downX));
        }
    };

    CustomScrubGestureDetector(final CustomSparkView customSparkView,
                               final CustomScrubGestureDetector.ScrubIndexListener scrubIndexListener,
                               final float touchSlop) {
        sparkView = customSparkView;
        scrubListener = customSparkView;
        this.scrubIndexListener = scrubIndexListener;
        this.touchSlop = touchSlop;
        handler = new Handler();
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

                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS);
                return true;
            case MotionEvent.ACTION_MOVE:
                // Calculate the elapsed time since the down event
                final float timeDelta = event.getEventTime() - event.getDownTime();

                // If the user has intentionally long-pressed
                if (timeDelta >= LONG_PRESS_TIMEOUT_MS) {
                    handler.removeCallbacks(longPressRunnable);
                    scrubListener.onScrubbed(x, y);
                    scrubIndexListener.onScrubbed(getNearestIndex(x));
                } else {
                    // If we moved before longpress, remove the callback if we exceeded the tap slop
                    float deltaX = x - downX;
                    float deltaY = y - downY;
                    if (deltaX >= touchSlop || deltaY >= touchSlop) {
                        /* We got a MOVE event that exceeded tap slop but before the long-press
                         * threshold, we don't care about this series of events anymore. */
                        handler.removeCallbacks(longPressRunnable);
                        return false;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                scrubListener.onScrubEnded();
                scrubIndexListener.onScrubEnded();
                return true;
            default:
                return false;
        }
    }

    private int getNearestIndex(final float x) {
        final List<Float> points = sparkView.getXPoints();

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
