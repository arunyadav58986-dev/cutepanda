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

        Log.d(TAG, "Initializing Unity Ads with Game ID: " + gameId);
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

        UnityAds.load(
                placementId,
                new UnityAdsLoadOptions(),
                new IUnityAdsLoadListener() {
                    @Override
                    public void onUnityAdsAdLoaded(String adUnitId) {
                        loadedPlacements.add(adUnitId);

                        JSObject data = new JSObject();
                        data.put("placementId", adUnitId);
                        notifyListeners("unityAdsLoaded", data);

                        call.resolve(data);
                    }

                    @Override
                    public void onUnityAdsFailedToLoad(String adUnitId, UnityAds.UnityAdsLoadError error, String message) {
                        loadedPlacements.remove(adUnitId);

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

        final Runnable doShow = new Runnable() {
            @Override
            public void run() {
                UnityAds.show(
                        activity,
                        placementId,
                        new UnityAdsShowOptions(),
                        new IUnityAdsShowListener() {
                            @Override
                            public void onUnityAdsShowFailure(String adUnitId, UnityAds.UnityAdsShowError error, String message) {
                                JSObject data = new JSObject();
                                data.put("placementId", adUnitId);
                                data.put("error", String.valueOf(error));
                                data.put("message", message);
                                notifyListeners("unityAdsShowFailed", data);
                            }

                            @Override
                            public void onUnityAdsShowStart(String adUnitId) {
                                JSObject data = new JSObject();
                                data.put("placementId", adUnitId);
                                notifyListeners("unityAdsShown", data);
                            }

                            @Override
                            public void onUnityAdsShowClick(String adUnitId) {
                                JSObject data = new JSObject();
                                data.put("placementId", adUnitId);
                                notifyListeners("unityAdsClicked", data);
                            }

                            @Override
                            public void onUnityAdsShowComplete(String adUnitId, UnityAds.UnityAdsShowCompletionState state) {
                                loadedPlacements.remove(adUnitId);

                                JSObject data = new JSObject();
                                data.put("placementId", adUnitId);
                                data.put("completionState", String.valueOf(state));
                                notifyListeners("unityAdsClosed", data);

                                if (rewardOnComplete && state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                                    notifyListeners("unityAdsRewarded", data);
                                }
                            }
                        }
                );
            }
        };

        if (loadedPlacements.contains(placementId)) {
            doShow.run();
            call.resolve();
            return;
        }

        UnityAds.load(
                placementId,
                new UnityAdsLoadOptions(),
                new IUnityAdsLoadListener() {
                    @Override
                    public void onUnityAdsAdLoaded(String adUnitId) {
                        loadedPlacements.add(adUnitId);
                        doShow.run();
                    }

                    @Override
                    public void onUnityAdsFailedToLoad(String adUnitId, UnityAds.UnityAdsLoadError error, String message) {
                        loadedPlacements.remove(adUnitId);

                        JSObject data = new JSObject();
                        data.put("placementId", adUnitId);
                        data.put("error", String.valueOf(error));
                        data.put("message", message);
                        notifyListeners("unityAdsLoadFailed", data);

                        call.reject(error + ": " + message);
                    }
                }
        );

        call.resolve();
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
