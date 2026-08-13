package com.fadi.vpn;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class VmessUpdateReceiver extends BroadcastReceiver {

    private static final long INTERVAL =
            12L * 60L * 60L * 1000L;

    private static final int REQUEST_CODE = 7788;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            scheduleNext(context);
            return;
        }

        try {
            VmessAccountManager manager =
                    new VmessAccountManager(context);

            new Thread(() -> {
                try {
                    manager.updateFromRemote();
                } catch (Throwable ignored) {
                }
            }).start();

        } catch (Throwable ignored) {
        }
    }

    private void scheduleNext(Context context) {
        try {
            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(
                            Context.ALARM_SERVICE
                    );

            if (alarmManager == null) {
                return;
            }

            Intent intent =
                    new Intent(
                            context,
                            VmessUpdateReceiver.class
                    );

            intent.setAction(
                    "com.fadi.vpn.UPDATE_VMESS"
            );

            int flags =
                    PendingIntent.FLAG_UPDATE_CURRENT;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            REQUEST_CODE,
                            intent,
                            flags
                    );

            long firstRun =
                    System.currentTimeMillis() + INTERVAL;

            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    firstRun,
                    INTERVAL,
                    pendingIntent
            );

        } catch (Throwable ignored) {
        }
    }
}
