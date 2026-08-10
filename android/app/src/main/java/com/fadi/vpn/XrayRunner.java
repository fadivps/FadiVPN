package com.fadi.vpn;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class XrayRunner {

    private final Context context;
    private Process process;

    public XrayRunner(Context context) {
        this.context = context;
    }

    public boolean start(String configPath) {
        try {
            File binary = new File(context.getFilesDir(), "libxray.so");

            if (!binary.exists()) {
                copyBinary(binary);
            }

            binary.setExecutable(true);

            ProcessBuilder pb = new ProcessBuilder(
                    binary.getAbsolutePath(),
                    "run",
                    "-c",
                    configPath
            );

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

    private void copyBinary(File destination) throws Exception {
        try (InputStream in = context.getAssets().open("libxray.so");
             FileOutputStream out = new FileOutputStream(destination)) {

            byte[] buffer = new byte[8192];
            int count;

            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        }
    }
}
