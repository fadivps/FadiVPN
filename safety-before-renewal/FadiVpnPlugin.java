package com.fadi.vpn;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;

import org.json.JSONObject;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "FadiVpn")
public class FadiVpnPlugin extends Plugin {

    private static final int VPN_REQUEST = 1001;
    private PluginCall pendingCall;

    @PluginMethod
    public void connect(PluginCall call) {
        Activity activity = getActivity();

        Intent prepareIntent = VpnService.prepare(activity);

        if (prepareIntent != null) {
            pendingCall = call;
            startActivityForResult(call, prepareIntent, VPN_REQUEST);
        } else {
            startVpn(call);
        }
    }

    @Override
    protected void handleOnActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.handleOnActivityResult(requestCode, resultCode, data);

        if (requestCode != VPN_REQUEST) {
            return;
        }

        PluginCall call = pendingCall;
        pendingCall = null;

        if (resultCode == Activity.RESULT_OK) {
            startVpn(call);
        } else if (call != null) {
            call.reject("VPN permission denied");
        }
    }

    private void startVpn(PluginCall call) {
        try {
            Intent intent = new Intent(getContext(), FadiVpnService.class);

            getContext().startService(intent);

            if (call != null) {
                JSObject result = new JSObject();
                result.put("started", true);
                call.resolve(result);
            }

        } catch (Exception e) {
            if (call != null) {
                call.reject("Failed to start VPN service", e);
            }
        }
    }

    @PluginMethod
    public void saveVmess(PluginCall call) {
        try {
            String link = call.getString("link");

            if (link == null || !link.startsWith("vmess://")) {
                call.reject("Invalid VMess link");
                return;
            }

            VmessAccountManager manager =
                    new VmessAccountManager(getContext());

            String prepared = manager.prepareVmess(link);

            JSObject result = new JSObject();
            result.put("saved", true);
            result.put("vmess", prepared);

            call.resolve(result);

        } catch (Exception e) {
            call.reject("Failed to save VMess", e);
        }
    }

    @PluginMethod
    public void getXrayMetrics(PluginCall call) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(
                        "http://127.0.0.1:49227/debug/vars"
                );

                java.net.HttpURLConnection connection =
                        (java.net.HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(2000);
                connection.setReadTimeout(3000);
                connection.setUseCaches(false);

                java.io.BufferedReader reader =
                        new java.io.BufferedReader(
                                new java.io.InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder body = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }

                reader.close();
                connection.disconnect();

                JSONObject json = new JSONObject(body.toString());

                JSObject result = new JSObject();
                result.put("metrics", json);

                call.resolve(result);

            } catch (Exception e) {
                call.reject("Failed to read Xray metrics", e);
            }
        }).start();
    }

    @PluginMethod
    public void getStats(PluginCall call) {
        try {
            long[] stats = readInterfaceStats("xray0");

            JSObject result = new JSObject();
            result.put("downloadBytes", stats[0]);
            result.put("uploadBytes", stats[1]);

            call.resolve(result);

        } catch (Exception e) {
            call.reject("Failed to read xray0 traffic stats", e);
        }
    }

    private long[] readInterfaceStats(String interfaceName) throws Exception {
        java.io.BufferedReader reader =
                new java.io.BufferedReader(
                        new java.io.FileReader("/proc/net/dev")
                );

        try {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (!line.startsWith(interfaceName + ":")) {
                    continue;
                }

                String data = line.substring(
                        line.indexOf(":") + 1
                ).trim();

                String[] fields = data.split("\\s+");

                if (fields.length < 9) {
                    throw new Exception("Invalid /proc/net/dev data");
                }

                long rxBytes = Long.parseLong(fields[0]);
                long txBytes = Long.parseLong(fields[8]);

                return new long[] { rxBytes, txBytes };
            }

        } finally {
            reader.close();
        }

        throw new Exception("Interface " + interfaceName + " not found");
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        try {
            Intent intent = new Intent(getContext(), FadiVpnService.class);
            intent.setAction("STOP_VPN");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(intent);
            } else {
                getContext().startService(intent);
            }

            JSObject result = new JSObject();
            result.put("stopped", true);
            call.resolve(result);

        } catch (Exception e) {
            call.reject("Failed to stop VPN service", e);
        }
    }
}
