package com.limelight.autoconnect;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * TVML: stores the host (and optionally the app) that should be connected
 * automatically when the TV turns on / the launcher activity starts.
 */
public class AutoConnectPrefs {
    private static final String PREF_FILE = "TvmlAutoConnect";
    private static final String KEY_UUID = "host_uuid";
    private static final String KEY_APP_ID = "app_id";
    private static final String KEY_APP_NAME = "app_name";
    private static final String KEY_APP_HDR = "app_hdr";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public static String getHostUuid(Context c) {
        return prefs(c).getString(KEY_UUID, null);
    }

    public static void setHost(Context c, String uuid) {
        // Changing the host invalidates the previously selected app
        prefs(c).edit().clear().putString(KEY_UUID, uuid).apply();
    }

    public static void clear(Context c) {
        prefs(c).edit().clear().apply();
    }

    /** @return app ID to launch, or 0 for "open the app list" */
    public static int getAppId(Context c) {
        return prefs(c).getInt(KEY_APP_ID, 0);
    }

    public static String getAppName(Context c) {
        return prefs(c).getString(KEY_APP_NAME, null);
    }

    public static boolean getAppHdr(Context c) {
        return prefs(c).getBoolean(KEY_APP_HDR, false);
    }

    public static void setApp(Context c, String uuid, int appId, String appName, boolean hdr) {
        prefs(c).edit()
                .putString(KEY_UUID, uuid)
                .putInt(KEY_APP_ID, appId)
                .putString(KEY_APP_NAME, appName)
                .putBoolean(KEY_APP_HDR, hdr)
                .apply();
    }

    public static void clearApp(Context c) {
        prefs(c).edit().remove(KEY_APP_ID).remove(KEY_APP_NAME).remove(KEY_APP_HDR).apply();
    }
}
