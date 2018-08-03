package c.chasesriprajittichai.stockwatch;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;


@AcraCore(buildConfigClass = BuildConfig.class)
public final class StockWatchApp extends Application {

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);
    }

}
