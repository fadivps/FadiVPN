package com.fadi.vpn;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FadiVpnPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
