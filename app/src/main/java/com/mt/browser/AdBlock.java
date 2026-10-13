package com.mt.browser;

public class AdBlock {
    static final String[] H = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "adservice.google.com", "amazon-adsystem.com",
        "adnxs.com", "criteo.com", "criteo.net", "taboola.com", "outbrain.com",
        "moatads.com", "scorecardresearch.com", "popads.net", "propellerads.com",
        "adsterra.com", "exoclick.com", "mgid.com", "pubmatic.com",
        "rubiconproject.com", "openx.net", "adcolony.com", "applovin.com",
        "unityads.unity3d.com", "ads.mopub.com", "hotjar.com", "quantserve.com"
    };
    public static boolean isAd(String host) {
        if (host == null) return false;
        for (String h : H) if (host.equals(h) || host.endsWith("." + h)) return true;
        return false;
    }
}
