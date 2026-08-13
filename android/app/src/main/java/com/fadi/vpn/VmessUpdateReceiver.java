package com.fadi.vpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class VmessUpdateReceiver extends BroadcastReceiver {

    public static final String ACTION_UPDATE =
            "com.fadi.vpn.ACTION_VMESS_UPDATE";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!ACTION_UPDATE.equals(intent.getAction())) {
            return;
        }

        try {
            VmessAccountManager manager =
                    new VmessAccountManager(context.getApplicationContext());

            /*
             * Safe behavior:
             *
             * - Check only when 12 hours have elapsed.
             * - Validate the remote configuration before replacing
             *   the currently stored configuration.
             * - Never restart the VPN from this receiver.
             * - If GitHub/network/update fails, the old configuration
             *   remains untouched.
             */
            manager.updateIfNeeded();

        } catch (Throwable ignored) {
            /*
             * Never allow background renewal failure to affect
             * the running VPN service.
             */
        }

        /*
         * Android may recreate the receiver/process later.
         * The next alarm is scheduled by MainActivity.
         */
    }
}
