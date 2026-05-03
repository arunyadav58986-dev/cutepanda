const StartioAds = window.Capacitor?.Plugins?.StartioAds;

const STARTIO_APP_ID = "204418927";

window.initAds = async function () {
  if (!StartioAds) {
    console.log("StartioAds plugin not found");
    return false;
  }

  try {
    const res = await StartioAds.init({
      appId: STARTIO_APP_ID,
    });

    console.log("Start.io init success:", res);

    try {
      await StartioAds.loadInterstitial();
      console.log("Start.io interstitial preload called");
    } catch (e) {
      console.log("Start.io preload failed:", e);
    }

    return true;
  } catch (e) {
    console.log("Start.io init failed:", e);
    return false;
  }
};

window.showInterstitial = async function () {
  if (!StartioAds) {
    console.log("StartioAds plugin not found");
    return false;
  }

  try {
    const ready = await StartioAds.isInterstitialReady();
    console.log("Start.io ready:", ready);

    if (!ready.loaded) {
      console.log("Interstitial not ready, loading now");
      try {
        await StartioAds.loadInterstitial();
      } catch (e) {
        console.log("Load failed:", e);
      }
      return false;
    }

    const res = await StartioAds.showInterstitial();
    console.log("Start.io show success:", res);
    return true;

  } catch (e) {
    console.log("Start.io show failed:", e);
    return false;
  }
};
