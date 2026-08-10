package com.fadi.vpn;

import android.content.Intent;
import android.net.VpnService;
import android.os.IBinder;

public class FadiVpnService extends VpnService {

    private Thread vpnThread;

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

                    android.os.ParcelFileDescriptor tun =
                            builder.establish();

                    if (tun == null) {
                        stopSelf();
                        return;
                    }

                    // إبقاء واجهة VPN فعالة.
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(1000);
                    }

                    tun.close();

                } catch (Exception e) {
                    e.printStackTrace();
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

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}
