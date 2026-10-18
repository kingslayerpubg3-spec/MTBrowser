package com.mt.browser;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.EditText;

public class BrowserHooks {
    static boolean loadingState;
    static boolean asking;

    static void lock(EditText e, String u) {
        Context c = e.getContext();
        int ic = (u == null || u.startsWith("file:") || u.startsWith("about:") || u.startsWith("data:")) ? R.drawable.ic_search
                : u.startsWith("https:") ? R.drawable.ic_lock : R.drawable.ic_unlock;
        Drawable l = c.getDrawable(ic);
        Drawable r = c.getDrawable(loadingState ? R.drawable.ic_m_close : R.drawable.ic_m_refresh).mutate();
        r.setTint(UiTheme.sub(c));
        e.setCompoundDrawablesRelativeWithIntrinsicBounds(l, null, r, null);
    }

    static String base(String h) {
        if (h == null) return "";
        String[] p = h.toLowerCase().split("\\.");
        int n = p.length;
        if (n <= 2) return h.toLowerCase();
        if (p[n - 1].length() == 2 && p[n - 2].length() <= 3) return p[n - 3] + "." + p[n - 2] + "." + p[n - 1];
        return p[n - 2] + "." + p[n - 1];
    }

    static boolean nav(final MainActivity a, final WebView v, WebResourceRequest r) {
        if (!Prefs.askAds(a) || !r.isForMainFrame() || r.hasGesture()) return false;
        final String u = r.getUrl().toString();
        String cu = v.getUrl();
        if (!u.startsWith("http") || cu == null || !cu.startsWith("http")) return false;
        String h2 = r.getUrl().getHost();
        if (base(h2).equals(base(Uri.parse(cu).getHost()))) return false;
        if (asking) return true;
        asking = true;
        new AlertDialog.Builder(a).setTitle("Site aapko dusri jagah bhej rahi hai")
            .setMessage(h2 + "\n\nBina tap ke redirect ho raha hai (ad ho sakta hai). Kholna hai?")
            .setPositiveButton("Allow", (d, w) -> { asking = false; v.loadUrl(u); })
            .setNegativeButton("Block", (d, w) -> asking = false)
            .setOnCancelListener(d -> asking = false).show();
        return true;
    }
}
