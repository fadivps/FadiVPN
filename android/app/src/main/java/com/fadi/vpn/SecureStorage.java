package com.fadi.vpn;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class SecureStorage {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "FadiVPN_AES_KEY";
    private static final String PREFS = "FadiVPN_SECURE";
    private static final String KEY_DATA = "encrypted_data";

    private SecureStorage() {}

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);

            if (entry instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            }
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE
        );

        generator.init(
                new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT |
                        KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build()
        );

        return generator.generateKey();
    }

    public static void put(Context context, String value) throws Exception {
        if (value == null) {
            remove(context);
            return;
        }

        SecretKey key = getOrCreateKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encrypted = cipher.doFinal(
                value.getBytes(StandardCharsets.UTF_8)
        );

        byte[] iv = cipher.getIV();

        byte[] combined = new byte[iv.length + encrypted.length];

        System.arraycopy(
                iv, 0,
                combined,
                0,
                iv.length
        );

        System.arraycopy(
                encrypted,
                0,
                combined,
                iv.length,
                encrypted.length
        );

        String encoded = Base64.getEncoder()
                .encodeToString(combined);

        context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        )
        .edit()
        .putString(KEY_DATA, encoded)
        .apply();
    }

    public static String get(Context context) throws Exception {

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        String encoded = prefs.getString(KEY_DATA, null);

        if (encoded == null) {
            return null;
        }

        byte[] combined = Base64.getDecoder()
                .decode(encoded);

        int ivLength = 12;

        if (combined.length <= ivLength) {
            return null;
        }

        byte[] iv = new byte[ivLength];

        byte[] encrypted =
                new byte[combined.length - ivLength];

        System.arraycopy(
                combined,
                0,
                iv,
                0,
                ivLength
        );

        System.arraycopy(
                combined,
                ivLength,
                encrypted,
                0,
                encrypted.length
        );

        SecretKey key = getOrCreateKey();

        Cipher cipher =
                Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                new GCMParameterSpec(128, iv)
        );

        byte[] decrypted =
                cipher.doFinal(encrypted);

        return new String(
                decrypted,
                StandardCharsets.UTF_8
        );
    }

    public static void remove(Context context) {
        context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        )
        .edit()
        .remove(KEY_DATA)
        .apply();
    }
}
