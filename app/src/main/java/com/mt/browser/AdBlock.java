package com.mt.browser;

import java.util.HashSet;

public class AdBlock {
    static final HashSet<String> S = new HashSet<>();

    static {
        String[] h = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com", "googletagservices.com",
            "google-analytics.com", "adservice.google.com", "amazon-adsystem.com", "adnxs.com", "adsrvr.org",
            "criteo.com", "criteo.net", "taboola.com", "outbrain.com", "moatads.com", "scorecardresearch.com",
            "quantserve.com", "popads.net", "popcash.net", "propellerads.com", "adsterra.com", "exoclick.com",
            "exosrv.com", "exdynsrv.com", "juicyads.com", "trafficjunky.net", "trafficfactory.biz", "realsrv.com",
            "tsyndicate.com", "magsrv.com", "clickadu.com", "hilltopads.net", "onclickads.net", "ad-maven.com",
            "a-ads.com", "ero-advertising.com", "plugrush.com", "mgid.com", "pubmatic.com", "rubiconproject.com",
            "openx.net", "adcolony.com", "applovin.com", "unityads.unity3d.com", "ads.mopub.com", "hotjar.com",
            "adform.net", "smartadserver.com", "casalemedia.com", "indexww.com", "33across.com", "sharethrough.com",
            "teads.tv", "yieldmo.com", "adroll.com", "bidswitch.net", "contextweb.com", "lijit.com", "sovrn.com",
            "triplelift.com", "advertising.com", "serving-sys.com", "2mdn.net", "doubleverify.com",
            "adsafeprotected.com", "zedo.com", "revcontent.com", "adtechus.com"
        };
        for (String x : h) S.add(x);
    }

    public static boolean isAd(String host) {
        if (host == null || host.isEmpty()) return false;
        String h = host.toLowerCase();
        while (true) {
            if (S.contains(h)) return true;
            int i = h.indexOf('.');
            if (i < 0 || h.indexOf('.', i + 1) < 0) return false;
            h = h.substring(i + 1);
        }
    }
}
