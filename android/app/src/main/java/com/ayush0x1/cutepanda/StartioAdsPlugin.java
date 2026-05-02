package com.ayush0x1.cutepanda;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

@CapacitorPlugin(name = "StartioAds")
public class StartioAdsPlugin extends Plugin {

    @PluginMethod
    public void init(PluginCall call) {
        String appId = call.getString("appId");

        if (appId == null || appId.isEmpty()) {
            call.reject("Missing Start.io appId");
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                StartAppSDK.setTestAdsEnabled(true); // testing only
                StartAppSDK.init(getActivity(), appId, false);
                StartAppAd.disableSplash();
                call.resolve();
            } catch (Exception e) {
                call.reject("Start.io init failed: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void showInterstitial(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                boolean shown = StartAppAd.showAd(getActivity());

                if (shown) {
                    call.resolve();
                } else {
                    call.reject("Ad not ready");
                }
            } catch (Exception e) {
                call.reject("Start.io ad error: " + e.getMessage());
            }
        });
    }
}
