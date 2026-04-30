import {
  AdMob,
  BannerAdSize,
  BannerAdPosition,
} from "@capacitor-community/admob";

// INIT
export async function initAds() {
  await AdMob.initialize({
    requestTrackingAuthorization: true,
  });
}

// BANNER
export async function showBanner() {
  await AdMob.showBanner({
    adId: "ca-app-pub-4986355696565453/2572080211",
    adSize: BannerAdSize.BANNER,
    position: BannerAdPosition.BOTTOM_CENTER,
    isTesting: false,
  });
}

// INTERSTITIAL (GAME OVER)
export async function showInterstitial() {
  await AdMob.prepareInterstitial({
    adId: "ca-app-pub-4986355696565453/7651867728",
    isTesting: false,
  });

  await AdMob.showInterstitial();
}
