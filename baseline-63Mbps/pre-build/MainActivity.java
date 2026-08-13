package com.fadi.vpn;

import android.os.Bundle;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private void scheduleVmessRenewal() {
        try {
            AlarmManager alarmManager =
                    (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            Intent intent =
                    new Intent(this, VmessUpdateReceiver.class);

            intent.setAction(
                    VmessUpdateReceiver.ACTION_UPDATE
            );

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            this,
                            1200,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_IMMUTABLE
                    );

            /*
             * Inexact repeating alarm:
             * no exact-alarm permission is required.
             *
             * 12 hours = 43,200,000 ms.
             */
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis()
                            + (12L * 60L * 60L * 1000L),
                    12L * 60L * 60L * 1000L,
                    pendingIntent
            );

        } catch (Throwable ignored) {
            /*
             * Scheduling failure must never prevent
             * the VPN application from starting.
             */
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FadiVpnPlugin.class);
        super.onCreate(savedInstanceState);

        /*
         * Schedule background VMess renewal.
         * This does not start, stop, or restart the VPN.
         */
        scheduleVmessRenewal();
    }
}
