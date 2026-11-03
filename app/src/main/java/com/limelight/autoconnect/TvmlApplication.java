package com.limelight.autoconnect;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

import com.limelight.LimeLog;

/**
 * TVML: Android TV sticks (e.g. Mi TV Stick 4K) usually go to standby instead of
 * powering off, so "TV turned on" does not mean a fresh boot / HOME launch.
 * The stream also ends when the stick goes to standby (Game.onStop), so on
 * SCREEN_ON we re-run the auto-connect flow.
 *
 * Starting an activity from the background is allowed here because this app is
 * meant to be the default HOME app, which is exempt from background-start limits.
 */
public class TvmlApplication extends Application {
    private static final long MIN_SCREEN_OFF_MS = 5000;
    private long screenOffAt;

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    screenOffAt = SystemClock.elapsedRealtime();
                    return;
                }

                // Ignore short blips (e.g. HDMI renegotiation)
                if (screenOffAt == 0 || SystemClock.elapsedRealtime() - screenOffAt < MIN_SCREEN_OFF_MS) {
                    return;
                }
                screenOffAt = 0;

                if (AutoConnectPrefs.getHostUuid(context) == null) {
                    return;
                }

                LimeLog.info("Screen on: starting auto-connect");
                try {
                    Intent i = new Intent(context, AutoConnectActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                } catch (RuntimeException e) {
                    LimeLog.warning("Unable to start auto-connect on screen on: " + e.getMessage());
                }
            }
        }, filter);
    }
}
