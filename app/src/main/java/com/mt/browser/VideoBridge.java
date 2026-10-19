package com.mt.browser;

import android.webkit.JavascriptInterface;

public class VideoBridge {
    final MainActivity a;
    final MainActivity.Tab t;

    VideoBridge(MainActivity a, MainActivity.Tab t) { this.a = a; this.t = t; }

    @JavascriptInterface
    public void video(String url, String title) {
        t.vurl = url == null ? "" : url;
        t.vtitle = title == null ? "" : title;
        if (t.vurl.startsWith("http") && !t.vurl.toLowerCase().contains(".m3u8")) ProbeCache.async(t.vurl);
        a.runOnUiThread(a::updateDlFab);
    }

    @JavascriptInterface
    public void gone() {
        t.vplaying = false;
        a.runOnUiThread(a::updateDlFab);
    }

    @JavascriptInterface
    public void dl(String url, String title) {
        t.vurl = url == null ? "" : url;
        t.vtitle = title == null ? "" : title;
        a.runOnUiThread(a::onDlFab);
    }

    @JavascriptInterface
    public String accent() {
        return String.format("#%06X", 0xFFFFFF & UiTheme.acc(a));
    }

    @JavascriptInterface
    public boolean canOpen() { return Prefs.builtinPlayer(a); }

    @JavascriptInterface
    public boolean open(String src, String title, double pos) {
        final String u = a.bestVideoUrl(t, src);
        if (u == null) return false;
        final String ti = title;
        final double p = pos;
        a.runOnUiThread(() -> a.openPlayer(u, ti, p, t));
        return true;
    }

    @JavascriptInterface
    public boolean inline(String src, String title, double pos, double dur, double dpr, double l, double tp, double w, double h) {
        if (!Prefs.builtinPlayer(a)) return false;
        final java.util.ArrayList<String> c = a.candidates(t, src);
        if (c.isEmpty()) return false;
        final String ti = title;
        final double p = pos, d = dur, dp = dpr, x = l, y = tp, ww = w, hh = h;
        a.runOnUiThread(() -> a.inlineStart(t, c, ti, p, d, dp, x, y, ww, hh));
        return true;
    }

    @JavascriptInterface
    public void rect(double dpr, double l, double tp, double w, double h, boolean vis) {
        final double dp = dpr, x = l, y = tp, ww = w, hh = h;
        final boolean v = vis;
        a.runOnUiThread(() -> a.inlineMove(t, dp, x, y, ww, hh, v));
    }

    @JavascriptInterface
    public void vclose() { a.runOnUiThread(() -> a.inlineStop(t, false)); }

    @JavascriptInterface
    public void media(String json) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                String u = arr.getString(i);
                try {
                    if (AdBlock.isAd(android.net.Uri.parse(u).getHost())) continue;
                } catch (Exception e) { }
                synchronized (t.scraped) { if (t.scraped.size() < 60) t.scraped.add(u); }
            }
        } catch (Exception e) { }
    }
}
