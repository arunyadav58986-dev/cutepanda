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
    private String initCallId = null;

    @PluginMethod
    public void init(PluginCall call) {
        // Prevent double initialization
        if (initialized) {
            call.resolve();
            return;
        }

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

        initCallId = call.getCallbackId();
        saveCall(call);

        // Unity Ads init is safe on background thread, but standard to call from activity context
        UnityAds.initialize(activity.getApplicationContext(), gameId, testMode, this);
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

        if (placementId == null || placementId.isEmpty()) {
            call.reject("placementId is required");
            return;
        }

        if (!initialized) {
            call.reject("Unity Ads not initialized");
            return;
        }

        UnityAds.load(placementId, new UnityAdsLoadOptions(), new IUnityAdsLoadListener() {
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
                call.reject(message, String.valueOf(error));
            }
        });
    }

    private void showPlacement(final PluginCall call, final boolean rewardOnComplete) {
        final String placementId = call.getString("placementId");
        final Activity activity = getActivity();

        if (activity == null || placementId == null) {
            call.reject("Activity or Placement ID missing");
            return;
        }

        final IUnityAdsShowListener showListener = new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String adUnitId, UnityAds.UnityAdsShowError error, String message) {
                loadedPlacements.remove(adUnitId);
                call.reject(message, String.valueOf(error));
            }

            @Override
            public void onUnityAdsShowStart(String adUnitId) {
                notifyListeners("unityAdsShown", new JSObject().put("placementId", adUnitId));
            }

            @Override
            public void onUnityAdsShowClick(String adUnitId) {}

            @Override
            public void onUnityAdsShowComplete(String adUnitId, UnityAds.UnityAdsShowCompletionState state) {
                loadedPlacements.remove(adUnitId);
                JSObject data = new JSObject();
                data.put("placementId", adUnitId);
                data.put("completionState", String.valueOf(state));
                
                if (rewardOnComplete && state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    notifyListeners("unityAdsRewarded", data);
                }
                
                call.resolve(data);
            }
        };

        // CRITICAL FIX: Wrap UnityAds.show in runOnUiThread
        activity.runOnUiThread(() -> {
            if (loadedPlacements.contains(placementId)) {
                UnityAds.show(activity, placementId, new UnityAdsShowOptions(), showListener);
            } else {
                // If not loaded, try to load it first
                UnityAds.load(placementId, new UnityAdsLoadOptions(), new IUnityAdsLoadListener() {
                    @Override
                    public void onUnityAdsAdLoaded(String adUnitId) {
                        loadedPlacements.add(adUnitId);
                        
                        // FIX APPLIED HERE: Ensure this inner show() runs on the UI thread as well
                        activity.runOnUiThread(() -> {
                            UnityAds.show(activity, adUnitId, new UnityAdsShowOptions(), showListener);
                        });
                    }

                    @Override
                    public void onUnityAdsFailedToLoad(String adUnitId, UnityAds.UnityAdsLoadError error, String message) {
                        call.reject("Ad not loaded and failed to load: " + message);
                    }
                });
            }
        });
    }

    @Override
    public void onInitializationComplete() {
        initialized = true;
        if (initCallId != null) {
            PluginCall savedCall = getSavedCall(initCallId);
            if (savedCall != null) {
                savedCall.resolve();
                releaseCall(savedCall);
            }
        }
        notifyListeners("unityAdsInitialized", new JSObject());
    }

    @Override
    public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
        initialized = false;
        if (initCallId != null) {
            PluginCall savedCall = getSavedCall(initCallId);
            if (savedCall != null) {
                savedCall.reject(message, String.valueOf(error));
                releaseCall(savedCall);
            }
        }
    }
}
