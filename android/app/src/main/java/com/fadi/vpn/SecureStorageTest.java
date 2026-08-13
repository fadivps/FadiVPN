package com.fadi.vpn;

import android.content.Context;

public final class SecureStorageTest {

    private SecureStorageTest() {}

    public static boolean run(Context context) {
        try {
            String original = "FadiVPN_SECURE_TEST_2026";

            SecureStorage.put(context, original);

            String restored = SecureStorage.get(context);

            SecureStorage.remove(context);

            return original.equals(restored);

        } catch (Exception e) {
            return false;
        }
    }
}
