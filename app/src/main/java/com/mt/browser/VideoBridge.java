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
        t.vplaying = true;
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
}
