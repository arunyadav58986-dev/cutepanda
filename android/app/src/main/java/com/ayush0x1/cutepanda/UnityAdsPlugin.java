package com.ayush0x1.cutepanda;

import android.app.Activity;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;

import java.util.HashSet;
import java.util.Set;

@CapacitorPlugin(name = "UnityAds")
public class UnityAdsPlugin extends Plugin implements IUnityAdsInitializationListener {

    private static final String TAG = "UnityAdsPlugin";
    private final Set<String> loadedPlacements = new HashSet<>();
    private boolean initialized = false;

    @PluginMethod
    public void init(PluginCall call) {
        final String gameId = call.getString("gameId");
        final boolean testMode = call.getBoolean("testMode", true);

        if (gameId == null || gameId.trim().isEmpty()) {
            call.reject("gameId is required");
            return;
        }

        final Activity activity = getActivity();
        if (activity == null) {
            call.reject("Activity is null");
            return;
        }

        Log.d(TAG, "Initializing Unity Ads with Game ID: " + gameId + ", testMode=" + testMode);
        UnityAds.initialize(activity, gameId, testMode, this);
        call.resolve();
    }

    @PluginMethod
    public void loadRewarded(PluginCall call) {
        loadPlacement(call);
    }

    @PluginMethod
    public void showRewarded(PluginCall call) {
        showPlacement(call, true);
    }

    @PluginMethod
    public void loadInterstitial(PluginCall call) {
        loadPlacement(call);
    }

    @PluginMethod
    public void showInterstitial(PluginCall call) {
        showPlacement(call, false);
    }

    private void loadPlacement(final PluginCall call) {
        final String placementId = call.getString("placementId");

        if (placementId == null || placementId.trim().isEmpty()) {
            call.reject("placementId is required");
            return;
        }

        if (!initialized) {
            call.reject("Unity Ads not initialized");
            return;
        }

        Log.d(TAG, "Loading placement: " + placementId);

        UnityAds.load(
            placementId,
            new UnityAdsLoadOptions(),
            new IUnityAdsLoadListener() {
                @Override
                public void onUnityAdsAdLoaded(String adUnitId) {
                    loadedPlacements.add(adUnitId);

                    Log.d(TAG, "Ad loaded: " + adUnitId);

                    JSObject data = new JSObject();
                    data.put("placementId", adUnitId);
                    notifyListeners("unityAdsLoaded", data);

                    call.resolve(data);
                }

                @Override
                public void onUnityAdsFailedToLoad(String adUnitId, UnityAds.UnityAdsLoadError error, String message) {
                    loadedPlacements.remove(adUnitId);

                    Log.e(TAG, "Load failed: " + adUnitId + " " + error + " " + message);

                    JSObject data = new JSObject();
                    data.put("placementId", adUnitId);
                    data.put("error", String.valueOf(error));
                    data.put("message", message);
                    notifyListeners("unityAdsLoadFailed", data);

                    call.reject(error + ": " + message);
                }
            }
        );
    }

    private void showPlacement(final PluginCall call, final boolean rewardOnComplete) {
        final String placementId = call.getString("placementId");

        if (placementId == null || placementId.trim().isEmpty()) {
            call.reject("placementId is required");
            return;
        }

        if (!initialized) {
            call.reject("Unity Ads not initialized");
            return;
        }

        final Activity activity = getActivity();
        if (activity == null) {
            call.reject("Activity is null");
            return;
        }

        Log.d(TAG, "Show requested for placement: " + placementId);

        final IUnityAdsShowListener showListener = new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String adUnitId, UnityAds.UnityAdsShowError error, String message) {
                loadedPlacements.remove(adUnitId);

                Log.e(TAG, "Show failed: " + adUnitId + " " + error + " " + message);

                JSObject data = new JSObject();
                data.put("placementId", adUnitId);
                data.put("error", String.valueOf(error));
                data.put("message", message);
                notifyListeners("unityAdsShowFailed", data);

                call.reject(error + ": " + message);
            }

            @Override
            public void onUnityAdsShowStart(String adUnitId) {
                Log.d(TAG, "Show started: " + adUnitId);

                JSObject data = new JSObject();
                data.put("placementId", adUnitId);
                notifyListeners("unityAdsShown", data);
            }

            @Override
            public void onUnityAdsShowClick(String adUnitId) {
                Log.d(TAG, "Ad clicked: " + adUnitId);

                JSObject data = new JSObject();
                data.put("placementId", adUnitId);
                notifyListeners("unityAdsClicked", data);
            }

            @Override
            public void onUnityAdsShowComplete(String adUnitId, UnityAds.UnityAdsShowCompletionState state) {
                loadedPlacements.remove(adUnitId);

                Log.d(TAG, "Show complete: " + adUnitId + " state=" + state);

                JSObject data = new JSObject();
                data.put("placementId", adUnitId);
                data.put("completionState", String.valueOf(state));
                notifyListeners("unityAdsClosed", data);

                if (rewardOnComplete && state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    notifyListeners("unityAdsRewarded", data);
                }

                // Resolve ONLY when the ad is actually finished/closed
                call.resolve(data);
            }
        };

        // If already loaded, show immediately.
        if (loadedPlacements.contains(placementId)) {
            Log.d(TAG, "Placement already loaded, showing now: " + placementId);

            UnityAds.show(
                activity,
                placementId,
                new UnityAdsShowOptions(),
                showListener
            );
            return;
        }

        // Otherwise load first, then show.
        Log.d(TAG, "Placement not loaded, loading first: " + placementId);

        UnityAds.load(
            placementId,
            new UnityAdsLoadOptions(),
            new IUnityAdsLoadListener() {
                @Override
                public void onUnityAdsAdLoaded(String adUnitId) {
                    loadedPlacements.add(adUnitId);

                    Log.d(TAG, "Loaded before show: " + adUnitId);

                    UnityAds.show(
                        activity,
                        adUnitId,
                        new UnityAdsShowOptions(),
                        showListener
                    );
                }

                @Override
                public void onUnityAdsFailedToLoad(String adUnitId, UnityAds.UnityAdsLoadError error, String message) {
                    loadedPlacements.remove(adUnitId);

                    Log.e(TAG, "Load before show failed: " + adUnitId + " " + error + " " + message);

                    JSObject data = new JSObject();
                    data.put("placementId", adUnitId);
                    data.put("error", String.valueOf(error));
                    data.put("message", message);
                    notifyListeners("unityAdsLoadFailed", data);

                    call.reject(error + ": " + message);
                }
            }
        );
    }

    @Override
    public void onInitializationComplete() {
        initialized = true;
        notifyListeners("unityAdsInitialized", new JSObject());
        Log.d(TAG, "Unity Ads initialized");
    }

    @Override
    public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
        initialized = false;

        JSObject data = new JSObject();
        data.put("error", String.valueOf(error));
        data.put("message", message);
        notifyListeners("unityAdsInitFailed", data);

        Log.e(TAG, "Unity Ads init failed: " + error + " " + message);
    }
}
