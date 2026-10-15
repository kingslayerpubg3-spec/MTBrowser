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
}
