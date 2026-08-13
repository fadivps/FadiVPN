package com.fadi.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

public class FadiVpnService extends VpnService {

    private static final String CHANNEL_ID = "FadiVPN";
    private static final int NOTIFICATION_ID = 1001;

    private Thread vpnThread;
    private ParcelFileDescriptor tun;
    private XrayRunner xray;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && "STOP_VPN".equals(intent.getAction())) {
            trace("STOP_VPN_REQUESTED");

            if (vpnThread != null) {
                vpnThread.interrupt();
                vpnThread = null;
            }

            if (xray != null) {
                try {
                    xray.stop();
                } catch (Exception ignored) {
                }
                xray = null;
            }

            if (tun != null) {
                try {
                    tun.close();
                } catch (Exception ignored) {
                }
                tun = null;
            }

            stopForeground(true);
            stopSelf();

            return START_NOT_STICKY;
        }

        trace("SERVICE_STARTED");
        trace("SERVICE_STARTED");

        startForegroundNotification();

        if (vpnThread == null || !vpnThread.isAlive()) {

            vpnThread = new Thread(() -> {

                try {
                    trace("BEFORE_BUILDER");
                    trace("BEFORE_BUILDER");
                    Builder builder = new Builder();
                    trace("AFTER_BUILDER");
                    trace("AFTER_BUILDER");

                    builder.setSession("FadiVPN");
                    builder.setMtu(1500);

                    builder.addAddress("10.8.0.2", 32);
                    builder.addRoute("0.0.0.0", 0);

                    try {
                        builder.addDisallowedApplication(getPackageName());
                    } catch (Exception ignored) {
                    }

                    builder.setBlocking(false);

                    trace("BEFORE_ESTABLISH");
                    trace("BEFORE_ESTABLISH");
                    tun = builder.establish();
                    trace("AFTER_ESTABLISH_" + (tun != null));
                    trace("AFTER_ESTABLISH_" + (tun != null));

                    if (tun == null) {
                        stopSelf();
                        return;
                    }

                    trace("BEFORE_XRAY_RUNNER");
                    trace("BEFORE_XRAY_RUNNER");
                    xray = new XrayRunner(this, tun);
                    trace("AFTER_XRAY_RUNNER");
                    trace("AFTER_XRAY_RUNNER");

                    trace("BEFORE_XRAY_START");
                    trace("TUN_BEFORE_START_FD=" + tun.getFd()
                            + "_VALID=" + (tun.getFd() >= 0)
                            + "_DESC=" + tun.getFileDescriptor().valid());

                    boolean started = xray.start();

                    trace("AFTER_XRAY_START_" + started);
                    trace("TUN_AFTER_START_FD=" + tun.getFd()
                            + "_VALID=" + (tun.getFd() >= 0)
                            + "_DESC=" + tun.getFileDescriptor().valid());

                    if (!started) {
                        stopSelf();
                        return;
                    }

                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(1000);
                    }

                } catch (Throwable e) {
                    trace("XRAY_FATAL_" + e.getClass().getName() + "_" + String.valueOf(e.getMessage()));
                    e.printStackTrace();

                } finally {

                    if (xray != null) {
                        xray.stop();
                        xray = null;
                    }

                    if (tun != null) {
                        try {
                            tun.close();
                        } catch (Exception ignored) {
                        }
                        tun = null;
                    }
                }

            });

            vpnThread.start();
        }

        return START_NOT_STICKY;
    }

    private void startForegroundNotification() {
        try {
            trace("FG_BEFORE");

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "FadiVPN",
                        NotificationManager.IMPORTANCE_LOW
                );

                manager.createNotificationChannel(channel);
                trace("FG_CHANNEL_OK");
            }

            Notification.Builder builder;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }

            trace("FG_BUILDER_OK");

            Notification notification = builder
                    .setContentTitle("FadiVPN")
                    .setContentText("VPN قيد التشغيل")
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setOngoing(true)
                    .build();

            trace("FG_NOTIFICATION_OK");

            startForeground(NOTIFICATION_ID, notification);

            trace("FG_START_OK");

        } catch (Throwable e) {
            trace("FG_FATAL_" + e.getClass().getName() + "_" + String.valueOf(e.getMessage()));
            throw e;
        }
    }

    private void trace(String message) {
        try {
            java.io.File dir = new java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                    ),
                    "FadiVPN"
            );

            dir.mkdirs();

            java.io.File file = new java.io.File(dir, "trace.txt");

            java.io.FileWriter writer = new java.io.FileWriter(file, true);
            writer.write(message + "\\n");
            writer.close();

        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDestroy() {

        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }

        if (xray != null) {
            xray.stop();
            xray = null;
        }

        if (tun != null) {
            try {
                tun.close();
            } catch (Exception ignored) {
            }
            tun = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}
