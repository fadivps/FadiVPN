package com.fadi.vpn;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class VmessAccountManager {

    private static final String PREFS = "FadiVPN_VMESS";
    private static final String KEY_LINK = "vmess_link";
    private static final String KEY_TIME = "vmess_time";
    private static final String KEY_REMOTE_UPDATED = "remote_updated_at";

    private final SharedPreferences prefs;
    private final Context context;

    public VmessAccountManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void logRemote(String text) {
        try {
            File dir = new File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                    ),
                    "FadiVPN"
            );

            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, "remote_update.txt");

            FileWriter writer = new FileWriter(file, true);
            writer.write(text + "\n");
            writer.close();

        } catch (Exception ignored) {
        }
    }

    public void saveVmess(String vmessLink) {
        if (vmessLink == null || !vmessLink.startsWith("vmess://")) {
            throw new IllegalArgumentException("Invalid VMess link");
        }

        prefs.edit()
                .putString(KEY_LINK, vmessLink)
                .putLong(KEY_TIME, System.currentTimeMillis())
                .apply();
    }

    public void saveInitialVmess() {
        String link = "vmess://eyJhZGQiOiAidXMzLnZwcm94eXkub25saW5lIiwgImFpZCI6ICIwIiwgImFsbG93SW5zZWN1cmUiOiAxLCAiZXh0cmEiOiB7Im5vR1JQQ0hlYWRlciI6IGZhbHNlLCAic2NNYXhDb25jdXJyZW50UG9zdHMiOiAxMDAsICJzY01heEVhY2hQb3N0Qnl0ZXMiOiAxMDAwMDAwLCAic2NNaW5Qb3N0c0ludGVydmFsTXMiOiAzMCwgInhQYWRkaW5nQnl0ZXMiOiAiMTAwLTEwMDAifSwgImZwIjogInJhbmRvbSIsICJob3N0IjogIiIsICJpZCI6ICI5Yjk1MDg4My1jOTE2LTRiNWUtYmU5My0zOWQ2OTIwMzVlZjgiLCAibmV0IjogInhodHRwIiwgInBhdGgiOiAiL3hodHRwIiwgInBvcnQiOiA0NDMsICJwcyI6ICIoc3Noc3RvcmVzLWRodmpmaCkgLSBbVk1lc3MgLSBYSFRUUCBUTFNdIiwgInNjeSI6ICJhdXRvIiwgInNuaSI6ICJ1czMudnByb3h5eS5vbmxpbmUiLCAidGxzIjogInRscyIsICJ0eXBlIjogImF1dG8iLCAidiI6ICIyIn0=";
        saveVmess(link);
    }

    public boolean updateFromRemote() {

        HttpURLConnection connection = null;

        logRemote("UPDATE_START");

        try {

            URL url = new URL(
                    "https://raw.githubusercontent.com/fadivps/FadiVPN/master/config/vmess.json"
            );

            logRemote("URL_OK");

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setUseCaches(false);

            int code = connection.getResponseCode();

            logRemote("HTTP_CODE=" + code);

            if (code != HttpURLConnection.HTTP_OK) {
                logRemote("UPDATE_FAILED_HTTP");
                return false;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            StringBuilder body = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                body.append(line);
            }

            reader.close();

            logRemote("BODY_LENGTH=" + body.length());

            JSONObject remote = new JSONObject(body.toString());

            String remoteLink = remote.optString("vmess", null);
            String remoteUpdated = remote.optString("updated_at", "");

            logRemote("UPDATED_AT=" + remoteUpdated);

            if (remoteLink == null ||
                    !remoteLink.startsWith("vmess://")) {

                logRemote("UPDATE_FAILED_INVALID_VMESS");
                return false;
            }

            String savedRemoteUpdated =
                    prefs.getString(KEY_REMOTE_UPDATED, "");

            logRemote(
                    "OLD_UPDATED_AT=" + savedRemoteUpdated
            );

            if (remoteUpdated.equals(savedRemoteUpdated)) {

                logRemote("REMOTE_UNCHANGED");

                return false;
            }

            String prepared = convertAddress(remoteLink);

            prefs.edit()
                    .putString(KEY_LINK, prepared)
                    .putString(KEY_REMOTE_UPDATED, remoteUpdated)
                    .putLong(KEY_TIME, System.currentTimeMillis())
                    .apply();

            logRemote("REMOTE_UPDATED_SUCCESS");

            return true;

        } catch (Exception e) {

            logRemote(
                    "UPDATE_EXCEPTION=" +
                    e.getClass().getName() +
                    ":" +
                    String.valueOf(e.getMessage())
            );

            return false;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String getVmess() {
        return prefs.getString(KEY_LINK, null);
    }

    public long getSavedTime() {
        return prefs.getLong(KEY_TIME, 0);
    }

    public boolean hasAccount() {
        return getVmess() != null;
    }

    public boolean needsRenewal() {

        long saved = getSavedTime();

        if (saved == 0) {
            return true;
        }

        return System.currentTimeMillis() - saved >=
                12L * 60L * 60L * 1000L;
    }

    public String convertAddress(String vmessLink) throws Exception {

        if (vmessLink == null ||
                !vmessLink.startsWith("vmess://")) {

            throw new IllegalArgumentException(
                    "Invalid VMess link"
            );
        }

        String encoded =
                vmessLink.substring("vmess://".length());

        byte[] decoded =
                Base64.getDecoder().decode(encoded);

        JSONObject config =
                new JSONObject(
                        new String(
                                decoded,
                                StandardCharsets.UTF_8
                        )
                );

        config.put(
                "add",
                "drugshortage.jp"
        );

        String result =
                Base64.getEncoder().encodeToString(
                        config.toString()
                                .getBytes(StandardCharsets.UTF_8)
                );

        return "vmess://" + result;
    }

    public String prepareVmess(String vmessLink)
            throws Exception {

        String prepared =
                convertAddress(vmessLink);

        saveVmess(prepared);

        return prepared;
    }

    public String getServerName() {
        return "امنيـه الاردن";
    }
}
