package com.sienga.stockwatch;


import net.hockeyapp.android.CrashManagerListener;


/**
 * This is a wrapper class of {@link CrashManagerListener} that overrides {@link
 * #shouldAutoUploadCrashes()} to return true.
 */
public final class CustomCrashManagerListener extends CrashManagerListener {

    /**
     * Crashes are usually sent the next time the app starts. If this method
     * returns true, crashes will be sent without any user interaction,
     * otherwise a dialog will appear allowing the user to decide whether they
     * want to send the report or not.
     *
     * @return true so that crashes will be sent, without user input
     */
    @Override
    public boolean shouldAutoUploadCrashes() {
        return true;
    }

}
