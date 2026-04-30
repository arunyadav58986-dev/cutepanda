// NO import, NO export

async function initAds() {
  try {
    const { AdMob } = Capacitor.Plugins;

    await AdMob.initialize({
      requestTrackingAuthorization: true,
    });
  } catch (e) {
    console.log("initAds error:", e);
  }
}

async function showBanner() {
  try {
    const { AdMob } = Capacitor.Plugins;

    await AdMob.showBanner({
      adId: "ca-app-pub-4986355696565453/2572080211",
      position: "BOTTOM_CENTER"
    });
  } catch (e) {
    console.log("banner error:", e);
  }
}

async function showInterstitial() {
  try {
    const { AdMob } = Capacitor.Plugins;

    await AdMob.prepareInterstitial({
      adId: "ca-app-pub-4986355696565453/7651867728"
    });

    await AdMob.showInterstitial();
  } catch (e) {
    console.log("interstitial error:", e);
  }
}
