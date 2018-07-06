package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.util.AttributeSet;

import com.robinhood.spark.SparkView;

import c.chasesriprajittichai.stockwatch.recyclerview.CustomScrubGestureDetector;

/**
 * This is a wrapper class for com.robinhood.spark.SparkView that implements
 * CustomScrubGestureDetector.ScrubListener.
 * <p>
 * For more background information on this class, look at CustomScrubGestureDetector.
 */
public final class CustomSparkView extends SparkView implements CustomScrubGestureDetector.ScrubListener {

    public CustomSparkView(final Context context) {
        super(context);
    }

    public CustomSparkView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSparkView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomSparkView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

}
