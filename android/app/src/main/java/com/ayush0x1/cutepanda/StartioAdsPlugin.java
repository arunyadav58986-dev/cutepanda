package com.ayush0x1.cutepanda;

import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

@CapacitorPlugin(name = "StartioAds")
public class StartioAdsPlugin extends Plugin {

    private static final String TAG = "StartIO";

    private StartAppAd interstitialAd;
    private boolean initialized = false;
    private boolean interstitialLoaded = false;
    private boolean interstitialLoading = false;

    @PluginMethod
    public void init(PluginCall call) {
        String appId = call.getString("appId");

        if (appId == null || appId.trim().isEmpty()) {
            call.reject("Missing Start.io appId");
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                // false = LIVE ADS
                // true  = TEST ADS
                // Keep false for real impressions/revenue.
                StartAppSDK.setTestAdsEnabled(false);

                // false = disable return ads
                StartAppSDK.init(getActivity(), appId, false);

                // Disable Start.io splash ad
                StartAppAd.disableSplash();

                interstitialAd = new StartAppAd(getActivity());
                initialized = true;

                Log.d(TAG, "Start.io initialized with appId: " + appId);

                JSObject ret = new JSObject();
                ret.put("initialized", true);
                ret.put("testMode", false);
                ret.put("message", "Start.io initialized");
                call.resolve(ret);

                // Preload first ad after init
                loadInterstitialInternal(null);

            } catch (Exception e) {
                initialized = false;
                Log.e(TAG, "Start.io init failed", e);
                call.reject("Start.io init failed: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void loadInterstitial(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            loadInterstitialInternal(call);
        });
    }

    private void loadInterstitialInternal(PluginCall call) {
        if (!initialized || interstitialAd == null) {
            if (call != null) {
                call.reject("Start.io not initialized");
            }
            Log.e(TAG, "Cannot load: Start.io not initialized");
            return;
        }

        if (interstitialLoading) {
            if (call != null) {
                JSObject ret = new JSObject();
                ret.put("loading", true);
                ret.put("loaded", interstitialLoaded);
                ret.put("message", "Interstitial already loading");
                call.resolve(ret);
            }
            Log.d(TAG, "Interstitial already loading");
            return;
        }

        try {
            interstitialLoading = true;
            interstitialLoaded = false;

            Log.d(TAG, "Loading interstitial");

            interstitialAd.loadAd(StartAppAd.AdMode.AUTOMATIC, new AdEventListener() {
                @Override
                public void onReceiveAd(Ad ad) {
                    interstitialLoading = false;
                    interstitialLoaded = true;

                    Log.d(TAG, "Interstitial LOADED");

                    if (call != null) {
                        JSObject ret = new JSObject();
                        ret.put("loaded", true);
                        ret.put("message", "Interstitial loaded");
                        call.resolve(ret);
                    }
                }

                @Override
                public void onFailedToReceiveAd(Ad ad) {
                    interstitialLoading = false;
                    interstitialLoaded = false;

                    String error = "Unknown load error";
                    try {
                        if (ad != null && ad.getErrorMessage() != null) {
                            error = ad.getErrorMessage();
                        }
                    } catch (Exception ignored) {}

                    Log.e(TAG, "Interstitial FAILED to load: " + error);

                    if (call != null) {
                        call.reject("Interstitial failed to load: " + error);
                    }
                }
            });

        } catch (Exception e) {
            interstitialLoading = false;
            interstitialLoaded = false;

            Log.e(TAG, "Interstitial load exception", e);

            if (call != null) {
                call.reject("Interstitial load exception: " + e.getMessage());
            }
        }
    }

    @PluginMethod
    public void isInterstitialReady(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("initialized", initialized);
        ret.put("loaded", interstitialLoaded);
        ret.put("loading", interstitialLoading);
        call.resolve(ret);
    }

    @PluginMethod
    public void showInterstitial(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (!initialized || interstitialAd == null) {
                call.reject("Start.io not initialized");
                Log.e(TAG, "Cannot show: Start.io not initialized");
                return;
            }

            if (!interstitialLoaded) {
                Log.d(TAG, "Interstitial not ready. Loading now.");
                loadInterstitialInternal(null);
                call.reject("Interstitial not ready yet");
                return;
            }

            try {
                Log.d(TAG, "Showing interstitial");

                boolean showStarted = interstitialAd.showAd(new AdDisplayListener() {
                    @Override
                    public void adHidden(Ad ad) {
                        Log.d(TAG, "Interstitial CLOSED/HIDDEN");

                        // After one ad is closed, preload next ad
                        interstitialLoaded = false;
                        loadInterstitialInternal(null);
                    }

                    @Override
                    public void adDisplayed(Ad ad) {
                        Log.d(TAG, "Interstitial DISPLAYED - wait for SDK log: Sending impression");
                    }

                    @Override
                    public void adClicked(Ad ad) {
                        Log.d(TAG, "Interstitial CLICKED");
                    }

                    @Override
                    public void adNotDisplayed(Ad ad) {
                        Log.e(TAG, "Interstitial NOT DISPLAYED");

                        interstitialLoaded = false;
                        loadInterstitialInternal(null);
                    }
                });

                if (showStarted) {
                    JSObject ret = new JSObject();
                    ret.put("showStarted", true);
                    ret.put("message", "Interstitial show started");
                    call.resolve(ret);
                    Log.d(TAG, "Interstitial show request accepted");
                } else {
                    interstitialLoaded = false;
                    loadInterstitialInternal(null);

                    Log.e(TAG, "Interstitial show request rejected");
                    call.reject("Interstitial show request rejected");
                }

            } catch (Exception e) {
                interstitialLoaded = false;
                loadInterstitialInternal(null);

                Log.e(TAG, "Interstitial show exception", e);
                call.reject("Interstitial show exception: " + e.getMessage());
            }
        });
    }
}
