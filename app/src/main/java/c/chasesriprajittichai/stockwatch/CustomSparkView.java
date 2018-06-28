package c.chasesriprajittichai.stockwatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.robinhood.spark.SparkView;

/**
 * This is a wrapper class for com.robinhood.spark.SparkView that implements
 * CustomScrubGestureDetector.ScrubListener.
 * <p>
 * For more background information on this class, look at CustomScrubGestureDetector.
 */
public final class CustomSparkView extends SparkView implements CustomScrubGestureDetector.ScrubListener {

    public CustomSparkView(Context context) {
        super(context);
    }

    public CustomSparkView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSparkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomSparkView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

}
