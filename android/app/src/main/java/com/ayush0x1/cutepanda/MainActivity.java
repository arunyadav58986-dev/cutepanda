package com.ayush0x1.cutepanda;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(UnityAdsPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
