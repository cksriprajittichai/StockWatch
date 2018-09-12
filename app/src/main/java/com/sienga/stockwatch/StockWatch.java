package com.sienga.stockwatch;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;


@AcraCore(buildConfigClass = BuildConfig.class)
public final class StockWatch extends Application {

    /**
     * This method is overridden to initialize {@link ACRA}.
     *
     * @param base The base context to attach
     */
    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);
    }

}
