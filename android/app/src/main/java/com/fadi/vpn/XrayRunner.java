package com.fadi.vpn;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

public class XrayRunner {

    private final Context context;
    private final ParcelFileDescriptor tun;
    private Process process;

    public XrayRunner(Context context, ParcelFileDescriptor tun) {
        this.context = context;
        this.tun = tun;
    }

    public boolean start() {
        try {
            File binary = new File(context.getFilesDir(), "libxray.so");
            File config = new File(context.getFilesDir(), "xray.json");

            copyAssetIfNeeded("libxray.so", binary);
            copyAssetIfNeeded("xray.json", config);

            binary.setExecutable(true);

            ProcessBuilder pb = new ProcessBuilder(
                    binary.getAbsolutePath(),
                    "run",
                    "-c",
                    config.getAbsolutePath()
            );

            Map<String, String> env = pb.environment();
            env.put("XRAY_TUN_FD", String.valueOf(tun.getFd()));

            pb.redirectErrorStream(true);

            process = pb.start();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void stop() {
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    private void copyAssetIfNeeded(String assetName, File destination)
            throws Exception {

        if (destination.exists() && destination.length() > 0) {
            return;
        }

        try (
            InputStream in = context.getAssets().open(assetName);
            FileOutputStream out = new FileOutputStream(destination)
        ) {
            byte[] buffer = new byte[8192];
            int count;

            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        }
    }
}
