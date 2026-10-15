package com.mt.browser;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;

public class BrowserHooks {
    static ValueCallback<Uri[]> fcb;
    static PermissionRequest pendReq;
    static GeolocationPermissions.Callback pendGeo;
    static String pendGeoOrigin;
    static SwipeRefreshLayout pull;

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

    static boolean external(Activity a, String u) {
        if (u.startsWith("http:") || u.startsWith("https:") || u.startsWith("file:") || u.startsWith("about:")
                || u.startsWith("data:") || u.startsWith("javascript:") || u.startsWith("blob:")) return false;
        try {
            Intent i;
            if (u.startsWith("intent:")) {
                i = Intent.parseUri(u, Intent.URI_INTENT_SCHEME);
                i.addCategory(Intent.CATEGORY_BROWSABLE);
                i.setComponent(null);
                i.setSelector(null);
            } else {
                i = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
            }
            a.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(a, "Is link ko kholne wala app nahi mila", Toast.LENGTH_SHORT).show();
        }
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
        new AlertDialog.Builder(a).setTitle("Location")
            .setMessage(Uri.parse(origin).getHost() + " aapki location maang raha hai")
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

    static boolean createWindow(final MainActivity a, boolean gesture, Message msg) {
        if (!gesture) {
            Toast.makeText(a, "Popup block kiya", Toast.LENGTH_SHORT).show();
            return false;
        }
        final boolean inc = a.cur != null && a.cur.incog;
        WebView tmp = new WebView(a);
        final boolean[] done = {false};
        tmp.setWebViewClient(new WebViewClient() {
            void go(WebView v, String u) {
                if (done[0] || u == null || u.equals("about:blank")) return;
                done[0] = true;
                a.newTab(inc);
                a.cur.wv.loadUrl(u);
                v.post(v::destroy);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { go(v, r.getUrl().toString()); return true; }
            @Override public void onPageStarted(WebView v, String u, Bitmap b) { go(v, u); }
        });
        WebView.WebViewTransport t = (WebView.WebViewTransport) msg.obj;
        t.setWebView(tmp);
        msg.sendToTarget();
        return true;
    }

    static void addPull(final MainActivity a, android.widget.FrameLayout c) {
        ViewGroup p = (ViewGroup) c.getParent();
        int i = p.indexOfChild(c);
        ViewGroup.LayoutParams lp = c.getLayoutParams();
        p.removeView(c);
        SwipeRefreshLayout s = new SwipeRefreshLayout(a);
        s.addView(c, new ViewGroup.LayoutParams(-1, -1));
        p.addView(s, i, lp);
        s.setOnRefreshListener(() -> { if (a.cur != null) a.cur.wv.reload(); });
        s.setOnChildScrollUpCallback((par, child) -> a.cur != null && a.cur.wv.getScrollY() > 0);
        s.setColorSchemeColors(UiTheme.acc(a));
        pull = s;
    }

    static void stopPull() { if (pull != null) pull.setRefreshing(false); }

    static void lock(EditText e, String u) {
        int ic = (u == null || u.startsWith("file:") || u.startsWith("about:") || u.startsWith("data:")) ? R.drawable.ic_search
                : u.startsWith("https:") ? R.drawable.ic_lock : R.drawable.ic_unlock;
        e.setCompoundDrawablesWithIntrinsicBounds(ic, 0, 0, 0);
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
                    Intent s = new Intent(Intent.ACTION_SEND);
                    s.setType("text/plain");
                    s.putExtra(Intent.EXTRA_TEXT, link);
                    a.startActivity(Intent.createChooser(s, "Share"));
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
