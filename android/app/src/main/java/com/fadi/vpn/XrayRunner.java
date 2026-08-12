package com.fadi.vpn;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class XrayRunner {

    private static final String TAG = "FadiVPN-Xray";

    static {
        System.loadLibrary("xray");
        System.loadLibrary("fadi_xray");
    }

    private final Context context;
    private final ParcelFileDescriptor tun;

    public XrayRunner(Context context, ParcelFileDescriptor tun) {
        this.context = context;
        this.tun = tun;
    }

    private native String nativeInvoke(String request);

    public boolean start() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("xrayJson", createXrayConfig());

            JSONObject request = new JSONObject();
            request.put("apiVersion", 2);
            request.put("method", "runXray");
            request.put("payload", payload);

            Log.i(TAG, "Calling libXray runXray");

            String response = nativeInvoke(request.toString());

            Log.i(TAG, "libXray response: " + response);
            try {
                java.io.File dir = new java.io.File(
                        android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_DOWNLOADS
                        ),
                        "FadiVPN"
                );
                dir.mkdirs();

                java.io.File file = new java.io.File(dir, "xray_response.txt");
                java.io.FileWriter writer = new java.io.FileWriter(file, true);
                writer.write("RESPONSE=" + String.valueOf(response) + "\n");
                writer.close();
            } catch (Exception ignored) {}

            if (response == null) {
                Log.e(TAG, "libXray returned null");
                return false;
            }

            JSONObject result = new JSONObject(response);

            if (!result.optBoolean("success", false)) {
                Log.e(
                        TAG,
                        "Xray failed: " +
                        result.optString("error", "unknown error")
                );
                return false;
            }

            Log.i(TAG, "Xray started successfully");
            return true;

        } catch (Throwable e) {
            Log.e(TAG, "Xray start exception", e);
            return false;
        }
    }

    public void stop() {
        try {
            JSONObject request = new JSONObject();

            request.put("apiVersion", 2);
            request.put("method", "stopXray");
            request.put("payload", new JSONObject());

            String response = nativeInvoke(request.toString());

            Log.i(TAG, "Xray stop response: " + response);

        } catch (Throwable e) {
            Log.e(TAG, "Xray stop exception", e);
        }
    }

    private String createXrayConfig() throws Exception {

        JSONObject root = new JSONObject();

        root.put(
                "metrics",
                new JSONObject()
                        .put("listen", "127.0.0.1:49227")
        );

        root.put(
                "policy",
                new JSONObject()
                        .put(
                                "system",
                                new JSONObject()
                                        .put("statsInboundDownlink", true)
                                        .put("statsInboundUplink", true)
                                        .put("statsOutboundDownlink", true)
                                        .put("statsOutboundUplink", true)
                        )
        );

        root.put("stats", new JSONObject());

        root.put(
                "log",
                new JSONObject()
                        .put("loglevel", "info")
        );

        JSONObject env = new JSONObject();
        try { java.io.FileWriter fw = new java.io.FileWriter(new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "FadiVPN/tun_debug.txt"), true); fw.write("TUN_FD=" + tun.getFd() + " valid=" + (tun.getFd() >= 0) + " descriptorValid=" + tun.getFileDescriptor().valid() + "\n"); fw.close(); } catch (Exception ignored) {}
        env.put("xray.tun.fd", String.valueOf(tun.getFd()));
        root.put("env", env);

        JSONObject tunSettings = new JSONObject();
        tunSettings.put("name", "xray0");
        tunSettings.put("mtu", 1500);

        JSONObject inbound = new JSONObject();
        inbound.put("tag", "tun-in");
        inbound.put("protocol", "tun");
        inbound.put("settings", tunSettings);

        inbound.put(
                "sniffing",
                new JSONObject()
                        .put("enabled", true)
                        .put(
                                "destOverride",
                                new JSONArray()
                                        .put("http")
                                        .put("tls")
                                        .put("quic")
                        )
        );

        root.put(
                "inbounds",
                new JSONArray().put(inbound)
        );

        VmessAccountManager accountManager =
                new VmessAccountManager(context);

        // Check GitHub for the latest VMess before starting Xray.
        // The manager changes ADD only to drugshortage.jp.
        accountManager.updateFromRemote();

        if (!accountManager.hasAccount()) {
            accountManager.saveInitialVmess();
        }

        String vmessLink = accountManager.getVmess();

        if (vmessLink == null || !vmessLink.startsWith("vmess://")) {
            accountManager.saveInitialVmess();
            vmessLink = accountManager.getVmess();
        }

        if (accountManager.needsRenewal()) {
            Log.i(TAG, "VMess account needs renewal");
        }

        // Change ADD only. SNI and all other account data remain unchanged.
        vmessLink = accountManager.convertAddress(vmessLink);

        String encoded = vmessLink.substring("vmess://".length());
        byte[] decoded = Base64.getDecoder().decode(encoded);

        JSONObject vmess = new JSONObject(
                new String(decoded, StandardCharsets.UTF_8)
        );

        JSONObject user = new JSONObject();
        user.put("id", vmess.getString("id"));
        user.put("alterId", vmess.optInt("aid", 0));
        user.put("security", vmess.optString("scy", "auto"));

        JSONObject vnext = new JSONObject();
        vnext.put("address", vmess.getString("add"));
        vnext.put("port", vmess.optInt("port", 443));
        vnext.put(
                "users",
                new JSONArray().put(user)
        );

        JSONObject vmessSettings = new JSONObject();
        vmessSettings.put(
                "vnext",
                new JSONArray().put(vnext)
        );

        JSONObject tlsSettings = new JSONObject();
        tlsSettings.put(
                "serverName",
                vmess.optString("sni", "")
        );
        tlsSettings.put(
                "fingerprint",
                vmess.optString("fp", "random")
        );

        JSONObject xhttpSettings = new JSONObject();
        xhttpSettings.put(
                "path",
                vmess.optString("path", "/xhttp")
        );
        xhttpSettings.put("mode", "auto");

        JSONObject streamSettings = new JSONObject();
        streamSettings.put(
                "network",
                vmess.optString("net", "xhttp")
        );
        streamSettings.put(
                "security",
                vmess.optString("tls", "tls")
        );
        streamSettings.put("tlsSettings", tlsSettings);
        streamSettings.put("xhttpSettings", xhttpSettings);

        JSONObject outbound = new JSONObject();
        outbound.put("tag", "proxy");
        outbound.put("protocol", "vmess");
        outbound.put("settings", vmessSettings);
        outbound.put("streamSettings", streamSettings);

        JSONObject direct = new JSONObject();
        direct.put("tag", "direct");
        direct.put("protocol", "freedom");

        JSONObject block = new JSONObject();
        block.put("tag", "block");
        block.put("protocol", "blackhole");

        root.put(
                "outbounds",
                new JSONArray()
                        .put(outbound)
                        .put(direct)
                        .put(block)
        );

        JSONObject routing = new JSONObject();
        routing.put("domainStrategy", "IPIfNonMatch");

        JSONArray rules = new JSONArray();

        rules.put(
                new JSONObject()
                        .put("type", "field")
                        .put(
                                "inboundTag",
                                new JSONArray().put("tun-in")
                        )
                        .put("outboundTag", "proxy")
        );

        routing.put("rules", rules);
        root.put("routing", routing);

        return root.toString();
    }

}
