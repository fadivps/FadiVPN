package com.fadi.vpn;

import android.content.Intent;
import android.net.VpnService;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

public class FadiVpnService extends VpnService {

    private Thread vpnThread;
    private ParcelFileDescriptor tun;
    private XrayRunner xray;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (vpnThread == null || !vpnThread.isAlive()) {

            vpnThread = new Thread(() -> {

                try {
                    Builder builder = new Builder();

                    builder.setSession("FadiVPN");
                    builder.setMtu(1500);

                    builder.addAddress("10.8.0.2", 32);
                    builder.addRoute("0.0.0.0", 0);

                    builder.setBlocking(false);

                    tun = builder.establish();

                    if (tun == null) {
                        stopSelf();
                        return;
                    }

                    xray = new XrayRunner(this);

                    boolean started = xray.start(
                            getFilesDir().getAbsolutePath() + "/xray.json"
                    );

                    if (!started) {
                        stopSelf();
                        return;
                    }

                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(1000);
                    }

                } catch (Exception e) {
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

        return START_STICKY;
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
