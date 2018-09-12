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

import android.content.Context;
import android.util.AttributeSet;

import com.robinhood.spark.SparkView;


/**
 * This is a wrapper class of {@link SparkView} that implements {@link
 * CustomScrubGestureDetector.ScrubListener}.
 *
 * @see CustomScrubGestureDetector
 */
public final class CustomSparkView extends SparkView implements
        CustomScrubGestureDetector.ScrubListener {

    public CustomSparkView(final Context context) {
        super(context);
    }

    public CustomSparkView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSparkView(final Context context, final AttributeSet attrs,
                           final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomSparkView(final Context context, final AttributeSet attrs,
                           final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

}
