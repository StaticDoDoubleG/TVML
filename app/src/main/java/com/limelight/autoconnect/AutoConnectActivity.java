package com.limelight.autoconnect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

import com.limelight.AppView;
import com.limelight.LimeLog;
import com.limelight.PcView;
import com.limelight.R;
import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.nvstream.wol.WakeOnLanSender;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.UiHelper;

import java.io.IOException;
import java.util.ArrayList;

/**
 * TVML: entry point of the app (LAUNCHER / LEANBACK_LAUNCHER / HOME).
 *
 * If an auto-connect host is configured, it tries to reach that host (sending
 * Wake-on-LAN packets while it is offline). On success the stream (or the app
 * list) is started; on failure or timeout the PC list (PcView) is shown.
 */
public class AutoConnectActivity extends Activity {
    // Total time we wait for the host before falling back to the PC list
    private static final long CONNECT_TIMEOUT_MS = 30000;

    private String uuid;
    private ComputerManagerService.ComputerManagerBinder managerBinder;
    private ProgressDialog spinner;
    private boolean finished;
    private long lastWolMs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            fallbackToPcList(getString(R.string.tvml_autoconnect_failed));
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            final ComputerManagerService.ComputerManagerBinder localBinder =
                    (ComputerManagerService.ComputerManagerBinder) binder;
            new Thread() {
                @Override
                public void run() {
                    localBinder.waitForReady();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (finished) {
                                return;
                            }
                            managerBinder = localBinder;
                            beginPolling();
                        }
                    });
                }
            }.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            managerBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.notifyNewRootView(this);

        uuid = AutoConnectPrefs.getHostUuid(this);
        if (uuid == null) {
            // Nothing configured: behave like the stock launcher entry
            fallbackToPcList(null);
            return;
        }

        spinner = new ProgressDialog(this);
        spinner.setTitle(getString(R.string.conn_establishing_title));
        spinner.setMessage(getString(R.string.tvml_autoconnect_msg));
        spinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        spinner.setCancelable(true);
        spinner.setCanceledOnTouchOutside(false);
        spinner.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // BACK while connecting: skip auto-connect and show the PC list
                fallbackToPcList(null);
            }
        });
        spinner.show();

        bindService(new Intent(this, ComputerManagerService.class), serviceConnection,
                Service.BIND_AUTO_CREATE);
        handler.postDelayed(timeoutRunnable, CONNECT_TIMEOUT_MS);
    }

    private void beginPolling() {
        final ComputerDetails computer = managerBinder.getComputer(uuid);
        if (computer == null) {
            // Host was deleted from the list
            AutoConnectPrefs.clear(this);
            fallbackToPcList(getString(R.string.scut_pc_not_found));
            return;
        }

        managerBinder.invalidateStateForComputer(uuid);
        managerBinder.startPolling(new ComputerManagerListener() {
            @Override
            public void notifyComputerUpdated(final ComputerDetails details) {
                if (!uuid.equalsIgnoreCase(details.uuid)) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onHostUpdated(details);
                    }
                });
            }
        });
    }

    private void onHostUpdated(ComputerDetails details) {
        if (finished || managerBinder == null) {
            return;
        }

        switch (details.state) {
            case ONLINE:
                if (details.pairState == PairingManager.PairState.PAIRED) {
                    if (details.activeAddress == null) {
                        // Address not populated yet; wait for next poll update
                        break;
                    }
                    launchStream(details);
                } else {
                    fallbackToPcList(getString(R.string.scut_not_paired));
                }
                break;

            case OFFLINE:
                // Keep waking the PC until the timeout expires
                long now = SystemClock.elapsedRealtime();
                if (details.macAddress != null && (lastWolMs == 0 || now - lastWolMs > 3000)) {
                    lastWolMs = now;
                    final ComputerDetails wolTarget = details;
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                WakeOnLanSender.sendWolPacket(wolTarget);
                            } catch (IOException e) {
                                LimeLog.warning("Auto-connect WoL failed: " + e.getMessage());
                            }
                        }
                    }.start();
                }
                // Force a re-poll; the timeout handles giving up
                managerBinder.invalidateStateForComputer(uuid);
                break;

            default:
                // UNKNOWN: still polling
                break;
        }
    }

    private void launchStream(ComputerDetails details) {
        ArrayList<Intent> stack = new ArrayList<>();

        // PC list at the bottom of the task so "back" from the stream lands there
        Intent pcView = new Intent(this, PcView.class);
        pcView.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        stack.add(pcView);

        Intent appView = new Intent(this, AppView.class);
        appView.putExtra(AppView.NAME_EXTRA, details.name);
        appView.putExtra(AppView.UUID_EXTRA, details.uuid);
        stack.add(appView);

        int appId = AutoConnectPrefs.getAppId(this);
        if (details.runningGameId != 0) {
            // Something is already running on the host: resume it
            stack.add(ServerHelper.createStartIntent(this,
                    new NvApp(null, details.runningGameId, false), details, managerBinder));
        } else if (appId != 0) {
            stack.add(ServerHelper.createStartIntent(this,
                    new NvApp(AutoConnectPrefs.getAppName(this), appId, AutoConnectPrefs.getAppHdr(this)),
                    details, managerBinder));
        }

        finishWith(stack.toArray(new Intent[0]));
    }

    private void fallbackToPcList(String message) {
        if (finished) {
            return;
        }
        if (message != null) {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
        Intent i = new Intent(this, PcView.class);
        i.setAction(Intent.ACTION_MAIN);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        finishWith(new Intent[]{i});
    }

    private void finishWith(Intent[] intents) {
        if (finished) {
            return;
        }
        finished = true;
        cleanup();
        startActivities(intents);
        finish();
    }

    private void cleanup() {
        handler.removeCallbacks(timeoutRunnable);
        if (spinner != null) {
            spinner.dismiss();
            spinner = null;
        }
        if (managerBinder != null) {
            managerBinder.stopPolling();
            managerBinder = null;
        }
        try {
            unbindService(serviceConnection);
        } catch (IllegalArgumentException ignored) {
            // Never bound
        }
    }

    @Override
    public void onBackPressed() {
        // Let the user skip auto-connect
        fallbackToPcList(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!finished) {
            finished = true;
            cleanup();
        }
    }
}
