package com.mt.browser;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;

public class BrowserHooks {
    static boolean loadingState;
    static boolean asking;
    static ValueCallback<Uri[]> fcb;
    static PermissionRequest pendReq;
    static GeolocationPermissions.Callback pendGeo;
    static String pendGeoOrigin;

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

    static boolean fileChooser(Activity a, ValueCallback<Uri[]> cb, WebChromeClient.FileChooserParams p) {
        if (fcb != null) fcb.onReceiveValue(null);
        fcb = cb;
        try {
            a.startActivityForResult(p.createIntent(), 88);
        } catch (Exception e) {
            fcb = null;
            return false;
        }
        return true;
    }

    static void onResult(int rc, int res, Intent d) {
        if (rc != 88 || fcb == null) return;
        fcb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, d));
        fcb = null;
    }

    static boolean externalOpen(Activity a, String u) {
        try {
            Intent i;
            if (u.startsWith("intent:")) {
                i = Intent.parseUri(u, Intent.URI_INTENT_SCHEME);
                i.addCategory(Intent.CATEGORY_BROWSABLE);
                i.setComponent(null);
                i.setSelector(null);
            } else i = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
            a.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(a, "Is link ko kholne wala app nahi mila", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    static boolean external(final Activity a, final String u) {
        String l = u.toLowerCase();
        if (l.startsWith("http:") || l.startsWith("https:") || l.startsWith("file:") || l.startsWith("about:")
                || l.startsWith("data:") || l.startsWith("javascript:") || l.startsWith("blob:") || l.startsWith("view-source:")) return false;
        if (!Prefs.askAds(a) || l.startsWith("tel:") || l.startsWith("mailto:") || l.startsWith("sms:")) return externalOpen(a, u);
        String sc = u.contains(":") ? u.substring(0, u.indexOf(':')) : u;
        new AlertDialog.Builder(a).setTitle("Dusra app kholna hai?")
            .setMessage("Ye site \"" + sc + "\" link se koi app kholna chahti hai. Allow karein?")
            .setPositiveButton("Allow", (d, w) -> externalOpen(a, u))
            .setNegativeButton("Block", null).show();
        return true;
    }

    static void permission(final Activity a, final PermissionRequest r) {
        final ArrayList<String> need = new ArrayList<>();
        for (String res : r.getResources()) {
            if (res.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) need.add(android.Manifest.permission.CAMERA);
            else if (res.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) need.add(android.Manifest.permission.RECORD_AUDIO);
        }
        if (need.isEmpty()) { r.deny(); return; }
        String what = need.size() == 2 ? "camera aur mic" : need.get(0).contains("CAMERA") ? "camera" : "mic";
        new AlertDialog.Builder(a).setTitle("Permission")
            .setMessage(r.getOrigin().getHost() + " ko " + what + " chahiye")
            .setPositiveButton("Allow", (d, w) -> {
                boolean all = true;
                for (String p : need) if (a.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) all = false;
                if (all) r.grant(r.getResources());
                else { pendReq = r; a.requestPermissions(need.toArray(new String[0]), 91); }
            })
            .setNegativeButton("Block", (d, w) -> r.deny())
            .setOnCancelListener(d -> r.deny()).show();
    }

    static void geo(final Activity a, final String origin, final GeolocationPermissions.Callback cb) {
        String h = Uri.parse(origin).getHost();
        new AlertDialog.Builder(a).setTitle("Location permission")
            .setMessage((h == null ? origin : h) + " aapki location maang raha hai")
            .setPositiveButton("Allow", (d, w) -> {
                if (a.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    cb.invoke(origin, true, false);
                else {
                    pendGeo = cb;
                    pendGeoOrigin = origin;
                    a.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 92);
                }
            })
            .setNegativeButton("Block", (d, w) -> cb.invoke(origin, false, false))
            .setOnCancelListener(d -> cb.invoke(origin, false, false)).show();
    }

    static void onPerm(int rc, int[] g) {
        boolean ok = g.length > 0;
        for (int x : g) if (x != PackageManager.PERMISSION_GRANTED) ok = false;
        if (rc == 91 && pendReq != null) {
            if (ok) pendReq.grant(pendReq.getResources()); else pendReq.deny();
            pendReq = null;
        }
        if (rc == 92 && pendGeo != null) {
            pendGeo.invoke(pendGeoOrigin, ok, false);
            pendGeo = null;
        }
    }

    static void ssl(Activity a, final SslErrorHandler h, SslError e) {
        new AlertDialog.Builder(a).setTitle("Connection safe nahi hai")
            .setMessage("Is site ka security certificate sahi nahi hai. Aapka data chori ho sakta hai.")
            .setPositiveButton("Wapas jao", (d, w) -> h.cancel())
            .setNegativeButton("Phir bhi kholo", (d, w) -> h.proceed())
            .setOnCancelListener(d -> h.cancel()).show();
    }

    static void error(Activity a, WebView v, WebResourceRequest r, WebResourceError e) {
        if (!r.isForMainFrame()) return;
        String u = r.getUrl().toString();
        String su = u.replace("'", "%27").replace("\"", "%22").replace("<", "%3C").replace("\\", "%5C");
        String acc = String.format("#%06X", 0xFFFFFF & UiTheme.acc(a));
        String bg = String.format("#%06X", 0xFFFFFF & UiTheme.bg2(a));
        String tx = String.format("#%06X", 0xFFFFFF & UiTheme.tx(a));
        String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
            + "<body style='margin:0;background:" + bg + ";color:" + tx + ";font-family:sans-serif;text-align:center;padding:90px 24px'>"
            + "<div style='font-size:64px'>&#128268;</div><h2>Page nahi khul paya</h2>"
            + "<p style='opacity:.7'>" + TextUtils.htmlEncode(String.valueOf(e.getDescription())) + "</p>"
            + "<button style='background:" + acc + ";color:#fff;border:0;border-radius:26px;padding:14px 34px;font-size:16px' "
            + "onclick=\"location.href='" + su + "'\">Dobara try karo</button></body></html>";
        v.loadDataWithBaseURL(u, html, "text/html", "utf-8", u);
    }

    static boolean createWindow(final MainActivity a, final boolean gesture, Message msg) {
        final boolean inc = a.cur != null && a.cur.incog;
        final boolean ask = Prefs.askAds(a);
        if (!gesture && !ask) {
            Toast.makeText(a, "Popup block kiya", Toast.LENGTH_SHORT).show();
            return false;
        }
        WebView tmp = new WebView(a);
        final boolean[] done = {false};
        tmp.setWebViewClient(new WebViewClient() {
            void go(final WebView v, final String u) {
                if (done[0] || u == null || u.equals("about:blank")) return;
                done[0] = true;
                v.stopLoading();
                v.post(v::destroy);
                final Runnable open = () -> { a.newTab(inc); a.cur.wv.loadUrl(u); };
                if (!ask) { open.run(); return; }
                String host = null;
                try { host = Uri.parse(u).getHost(); } catch (Exception e) { }
                new AlertDialog.Builder(a).setTitle("Naya tab kholna hai?")
                    .setMessage((gesture ? "Aapke tap par " : "Bina tap ke ") + "ye site naya tab kholna chahti hai:\n\n" + (host == null ? u : host))
                    .setPositiveButton("Allow", (d, w) -> open.run())
                    .setNegativeButton("Block", null).show();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { go(v, r.getUrl().toString()); return true; }
            @Override public void onPageStarted(WebView v, String u, Bitmap b) { go(v, u); }
        });
        WebView.WebViewTransport t = (WebView.WebViewTransport) msg.obj;
        t.setWebView(tmp);
        msg.sendToTarget();
        return true;
    }

    static void zoom(MainActivity a) {
        for (MainActivity.Tab t : a.tabs) t.wv.getSettings().setTextZoom(Prefs.textZoom(a));
    }

    static boolean longPress(final MainActivity a, final WebView wv) {
        WebView.HitTestResult r = wv.getHitTestResult();
        int t = r.getType();
        if (t == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            Handler h = new Handler(Looper.getMainLooper()) {
                @Override public void handleMessage(Message m) {
                    menu(a, m.getData().getString("url"), m.getData().getString("src"));
                }
            };
            wv.requestFocusNodeHref(h.obtainMessage());
            return true;
        }
        if (t == WebView.HitTestResult.SRC_ANCHOR_TYPE) { menu(a, r.getExtra(), null); return true; }
        if (t == WebView.HitTestResult.IMAGE_TYPE) { menu(a, null, r.getExtra()); return true; }
        return false;
    }

    static void menu(final MainActivity a, final String link0, final String img) {
        final String link = (link0 != null && !link0.startsWith("javascript:")) ? link0 : null;
        final ArrayList<String> n = new ArrayList<>();
        final ArrayList<Integer> k = new ArrayList<>();
        if (link != null && !link.isEmpty()) {
            n.add("Naye tab me kholo"); k.add(0);
            n.add("Incognito tab me kholo"); k.add(1);
            n.add("Link copy karo"); k.add(2);
            n.add("Link share karo"); k.add(3);
            n.add("Link download karo"); k.add(4);
        }
        if (img != null && !img.isEmpty()) {
            n.add("Image save karo"); k.add(5);
            n.add("Image naye tab me kholo"); k.add(6);
        }
        if (n.isEmpty()) return;
        String ti = (link != null && !link.isEmpty()) ? link : img;
        if (ti.length() > 60) ti = ti.substring(0, 60) + "...";
        final String ua = a.cur.wv.getSettings().getUserAgentString();
        new AlertDialog.Builder(a).setTitle(ti).setItems(n.toArray(new String[0]), (d, i) -> {
            switch (k.get(i)) {
                case 0: a.newTab(a.cur.incog); a.cur.wv.loadUrl(link); break;
                case 1: a.newTab(true); a.cur.wv.loadUrl(link); break;
                case 2:
                    ((ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("MT", link));
                    Toast.makeText(a, "Link copy ho gaya", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Intent sh = new Intent(Intent.ACTION_SEND);
                    sh.setType("text/plain");
                    sh.putExtra(Intent.EXTRA_TEXT, link);
                    a.startActivity(Intent.createChooser(sh, "Share"));
                    break;
                case 4: FastDownload.show(a, link, URLUtil.guessFileName(link, null, null), ua); break;
                case 5:
                    if (img.startsWith("data:")) Toast.makeText(a, "Is image ko save nahi kar sakte", Toast.LENGTH_SHORT).show();
                    else FastDownload.show(a, img, URLUtil.guessFileName(img, null, null), ua);
                    break;
                case 6: a.newTab(a.cur.incog); a.cur.wv.loadUrl(img); break;
            }
        }).show();
    }
}
