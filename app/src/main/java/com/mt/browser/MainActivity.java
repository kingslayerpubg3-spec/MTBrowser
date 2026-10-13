package com.mt.browser;

import android.app.DownloadManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    static final String HOME = "file:///android_asset/home.html";
    static final String INCOG = "file:///android_asset/incognito.html";
    static final String PROFILE = "incognito";

    static class Tab {
        WebView wv;
        boolean incog;
        String title = "New Tab";
    }

    ArrayList<Tab> tabs = new ArrayList<>();
    Tab cur;
    FrameLayout container;
    EditText urlBar;
    ProgressBar progress;
    Button btnTabs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.webContainer);
        urlBar = findViewById(R.id.urlBar);
        progress = findViewById(R.id.progress);
        btnTabs = findViewById(R.id.btnTabs);

        urlBar.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_GO || (e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                load(urlBar.getText().toString().trim());
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> { if (cur.wv.canGoBack()) cur.wv.goBack(); });
        findViewById(R.id.btnFwd).setOnClickListener(v -> { if (cur.wv.canGoForward()) cur.wv.goForward(); });
        findViewById(R.id.btnHome).setOnClickListener(v -> cur.wv.loadUrl(cur.incog ? INCOG : HOME));
        btnTabs.setOnClickListener(v -> showTabs());
        findViewById(R.id.btnMenu).setOnClickListener(this::showMenu);

        newTab(false);
    }

    boolean profilesSupported() {
        try { return WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE); }
        catch (Throwable e) { return false; }
    }

    void newTab(boolean incog) {
        final Tab t = new Tab();
        t.incog = incog;
        WebView w = new WebView(this);

        if (incog) {
            if (profilesSupported()) {
                try { WebViewCompat.setProfile(w, PROFILE); } catch (Throwable e) { }
            } else {
                Toast.makeText(this, "Is device me cookies fully alag nahi ho sakti", Toast.LENGTH_LONG).show();
            }
        }

        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        if (incog) s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        w.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) { return (Prefs.adBlock(MainActivity.this) && AdBlock.isAd(r.getUrl().getHost())) ? new WebResourceResponse("text/plain", "utf-8", new java.io.ByteArrayInputStream(new byte[0])) : null; }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return handleInternal(r.getUrl().toString()); }
            @Override public void onPageStarted(WebView v, String u, Bitmap f) {
                if (t == cur) urlBar.setText(u.startsWith("file:///android_asset") ? "" : u);
            }
            @Override public void onPageFinished(WebView v, String u) {
                String ti = v.getTitle(); webTheme(v);
                if (ti != null && !ti.isEmpty()) t.title = ti;
                if (!t.incog) Store.addHistory(MainActivity.this, t.title, u);
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback cb) { showFull(view, cb); }
            @Override public void onHideCustomView() { hideFull(); }
            @Override public void onProgressChanged(WebView v, int p) {
                if (t != cur) return;
                progress.setProgress(p);
                progress.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
            }
        });
        w.setDownloadListener((url, ua, cd, mime, len) -> {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            String name = URLUtil.guessFileName(url, cd, mime);
            r.setTitle(name);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
            Toast.makeText(this, "Downloading: " + name, Toast.LENGTH_SHORT).show();
        });

        t.wv = w;
        tabs.add(t);
        switchTo(t);
        w.loadUrl(incog ? INCOG : HOME);
    }

    void switchTo(Tab t) {
        container.removeAllViews();
        container.addView(t.wv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cur = t;
        String u = t.wv.getUrl();
        urlBar.setText(u == null || u.startsWith("file:///android_asset") ? "" : u);
        urlBar.setBackgroundResource(t.incog ? R.drawable.url_bg_incog : R.drawable.url_bg);
        progress.setVisibility(View.GONE);
        btnTabs.setText("[" + tabs.size() + "]");
        applyTheme();
    }

    void closeTab(Tab t) {
        int idx = tabs.indexOf(t);
        tabs.remove(t);
        container.removeAllViews();
        t.wv.destroy();

        boolean anyIncog = false;
        for (Tab x : tabs) if (x.incog) anyIncog = true;
        if (!anyIncog && profilesSupported()) {
            try { ProfileStore.getInstance().deleteProfile(PROFILE); } catch (Throwable e) { }
        }

        if (tabs.isEmpty()) newTab(false);
        else switchTo(tabs.get(Math.max(0, idx - 1)));
    }

    void showTabs() {
        String[] names = new String[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            names[i] = (t == cur ? "● " : "○ ") + (t.incog ? "🕶 " : "") + t.title;
        }
        new AlertDialog.Builder(this)
                .setTitle("Tabs (" + tabs.size() + ")")
                .setItems(names, (d, which) -> switchTo(tabs.get(which)))
                .setPositiveButton("+ Tab", (d, w) -> newTab(false))
                .setNegativeButton("+ Incognito", (d, w) -> newTab(true))
                .setNeutralButton("Close", null)
                .show();
    }

    void showMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, "New tab");
        pm.getMenu().add(0, 2, 1, "New incognito tab");
        pm.getMenu().add(0, 3, 2, "Close this tab");
        pm.getMenu().add(0, 4, 3, "Reload");
        pm.getMenu().add(0, 5, 4, "Add bookmark");
        pm.getMenu().add(0, 6, 5, "Bookmarks");
        pm.getMenu().add(0, 7, 6, "History");
        pm.getMenu().add(0, 8, 7, "Downloads");
        pm.getMenu().add(0, 9, 8, "Settings");
        pm.getMenu().add(0, 10, 9, "Desktop site");
        pm.getMenu().add(0, 11, 10, "Translate page");
        pm.getMenu().add(0, 12, 11, "Find in page");
        pm.getMenu().add(0, 13, 12, "Share");
        pm.getMenu().add(0, 14, 13, "Copy link");
        pm.getMenu().add(0, 15, 14, "Copy page text");
        pm.getMenu().add(0, 16, 15, "QR Scanner");
        pm.getMenu().add(0, 17, 16, "Smart Download");
        pm.getMenu().add(0, 18, 17, "Pop-up video (PiP)");
        pm.setOnMenuItemClickListener(i -> {
            switch (i.getItemId()) {
                case 1: newTab(false); break;
                case 2: newTab(true); break;
                case 3: closeTab(cur); break;
                case 4: cur.wv.reload(); break;
                case 5: addBookmark(); break;
                case 6: openLib("bookmarks"); break;
                case 7: openLib("history"); break;
                case 8: openLib("downloads"); break;
                case 9: openSettings(); break;
                case 10: toggleDesktop(); break;
                case 11: translate(); break;
                case 12: findInPage(); break;
                case 13: share(); break;
                case 14: copyLink(); break;
                case 15: copyText(); break;
                case 16: scanQr(); break;
                case 17: smartDownload(); break;
                case 18: enterPip(); break;
            }
            return true;
        });
        pm.show();
    }

    void load(String q) {
        if (q.isEmpty()) return;
        if (q.startsWith("http://") || q.startsWith("https://")) cur.wv.loadUrl(q);
        else if (q.contains(".") && !q.contains(" ")) cur.wv.loadUrl("https://" + q);
        else cur.wv.loadUrl(Prefs.searchUrl(this) + Uri.encode(q));
    }

    @Override
    public void onBackPressed() {
        if (fullView != null) { hideFull(); return; }
        if (cur.wv.canGoBack()) cur.wv.goBack();
        else if (tabs.size() > 1) closeTab(cur);
        else super.onBackPressed();
    }

    void openSettings() {
        startActivity(new android.content.Intent(this, SettingsActivity.class));
    }

    void toggleDesktop() {
        WebSettings s = cur.wv.getSettings();
        boolean isDesk = s.getUserAgentString().contains("X11; Linux x86_64");
        if (isDesk) {
            s.setUserAgentString(null);
            s.setUseWideViewPort(false);
            s.setLoadWithOverviewMode(false);
        } else {
            s.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
        }
        cur.wv.reload();
        Toast.makeText(this, isDesk ? "Mobile site" : "Desktop site", Toast.LENGTH_SHORT).show();
    }

    void translate() {
        final String u = cur.wv.getUrl();
        if (u == null || u.startsWith("file:")) {
            Toast.makeText(this, "Pehle koi website kholo", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] n = {"Hindi", "English", "Urdu", "Bengali", "Tamil", "Telugu", "Marathi", "Gujarati", "Punjabi"};
        final String[] c = {"hi", "en", "ur", "bn", "ta", "te", "mr", "gu", "pa"};
        new AlertDialog.Builder(this).setTitle("Translate to")
            .setItems(n, (d, i) -> cur.wv.loadUrl("https://translate.google.com/translate?sl=auto&tl=" + c[i] + "&u=" + Uri.encode(u)))
            .show();
    }

    void findInPage() {
        final EditText e = new EditText(this);
        e.setHint("Page me search karo");
        new AlertDialog.Builder(this).setTitle("Find in page").setView(e)
            .setPositiveButton("Find", (d, w) -> cur.wv.findAllAsync(e.getText().toString()))
            .setNegativeButton("Clear", (d, w) -> cur.wv.clearMatches())
            .show();
    }

    boolean isLight() {
        int t = Prefs.theme(this);
        if (t == 1) return true;
        if (t == 0) return false;
        return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                != android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    void applyTheme() {
        boolean l = isLight();
        findViewById(R.id.rootLayout).setBackgroundColor(l ? 0xFFF4F6FA : 0xFF050B1E);
        ViewGroup bb = findViewById(R.id.bottomBar);
        bb.setBackgroundColor(l ? 0xFFE3E8F2 : 0xFF0A1530);
        for (int i = 0; i < bb.getChildCount(); i++) {
            View c = bb.getChildAt(i);
            if (c instanceof Button && c.getId() != R.id.btnTabs)
                ((Button) c).setTextColor(l ? 0xFF1B2540 : 0xFFFFFFFF);
        }
        if (cur != null) {
            if (cur.incog) {
                urlBar.setBackgroundResource(R.drawable.url_bg_incog);
                urlBar.setTextColor(0xFFFFFFFF);
            } else {
                urlBar.setBackgroundResource(l ? R.drawable.url_bg_light : R.drawable.url_bg);
                urlBar.setTextColor(l ? 0xFF14213D : 0xFFFFFFFF);
            }
            webTheme(cur.wv);
        }
    }

    void webTheme(WebView w) {
        String u = w.getUrl();
        if (u != null && u.startsWith("file:///android_asset") && !u.contains("incognito"))
            w.evaluateJavascript("document.body.classList.toggle('light'," + isLight() + ")", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cur != null) applyTheme();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        if (cur != null) applyTheme();
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    void copy(String t) {
        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("MT Browser", t));
    }

    void share() {
        String u = cur.wv.getUrl();
        if (u == null || u.startsWith("file:")) { toast("Pehle koi website kholo"); return; }
        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(android.content.Intent.EXTRA_TEXT, u);
        startActivity(android.content.Intent.createChooser(i, "Share via"));
    }

    void copyLink() {
        String u = cur.wv.getUrl();
        if (u == null || u.startsWith("file:")) { toast("Pehle koi website kholo"); return; }
        copy(u);
        toast("Link copy ho gaya");
    }

    void copyText() {
        cur.wv.evaluateJavascript("(function(){var s=window.getSelection().toString();return s?s:document.body.innerText;})()", r -> {
            try {
                String t = String.valueOf(new org.json.JSONTokener(r).nextValue());
                copy(t.length() > 100000 ? t.substring(0, 100000) : t);
                toast("Text copy ho gaya");
            } catch (Exception e) { toast("Text copy nahi hua"); }
        });
    }

    void scanQr() {
        com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(this).startScan()
            .addOnSuccessListener(code -> {
                final String v = code.getRawValue();
                if (v == null) return;
                new AlertDialog.Builder(this).setTitle("QR Result").setMessage(v)
                    .setPositiveButton("Open", (d, w) -> load(v))
                    .setNegativeButton("Copy", (d, w) -> { copy(v); toast("Copy ho gaya"); })
                    .show();
            })
            .addOnFailureListener(e -> toast("Scanner nahi khula: " + e.getMessage()));
    }

    View fullView;
    WebChromeClient.CustomViewCallback fullCb;

    void showFull(View v, WebChromeClient.CustomViewCallback cb) {
        if (fullView != null) { cb.onCustomViewHidden(); return; }
        fullView = v;
        fullCb = cb;
        ((ViewGroup) getWindow().getDecorView()).addView(v, new ViewGroup.LayoutParams(-1, -1));
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    void hideFull() {
        if (fullView == null) return;
        ((ViewGroup) getWindow().getDecorView()).removeView(fullView);
        fullView = null;
        if (fullCb != null) fullCb.onCustomViewHidden();
        fullCb = null;
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    void enterPip() {
        try {
            enterPictureInPictureMode(new android.app.PictureInPictureParams.Builder()
                .setAspectRatio(new android.util.Rational(16, 9)).build());
        } catch (Exception e) { toast("PiP is device me nahi chal raha"); }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (fullView != null) enterPip();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean pip, android.content.res.Configuration c) {
        super.onPictureInPictureModeChanged(pip, c);
        int vis = pip ? View.GONE : View.VISIBLE;
        findViewById(R.id.urlBar).setVisibility(vis);
        findViewById(R.id.bottomBar).setVisibility(vis);
        if (pip) progress.setVisibility(View.GONE);
    }

    void smartDownload() {
        String js;
        try {
            java.io.InputStream in = getAssets().open("detect.js");
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            in.close();
            js = new String(bytes);
        } catch (Exception e) { toast("detect.js nahi mili"); return; }
        cur.wv.evaluateJavascript(js, r -> {
            try {
                String s = String.valueOf(new org.json.JSONTokener(r).nextValue());
                org.json.JSONArray a = new org.json.JSONArray(s);
                if (a.length() == 0) { toast("Is page par direct video/file nahi mila"); return; }
                final String[] urls = new String[a.length()];
                String[] names = new String[a.length()];
                for (int i = 0; i < a.length(); i++) {
                    org.json.JSONObject o = a.getJSONObject(i);
                    urls[i] = o.getString("u");
                    String l = o.getString("l");
                    names[i] = (l.isEmpty() ? "" : l + " • ") + URLUtil.guessFileName(urls[i], null, null);
                }
                new AlertDialog.Builder(this).setTitle("Smart Download (" + a.length() + ")")
                    .setItems(names, (d, i) -> startDownload(urls[i])).show();
            } catch (Exception e) { toast("Detect nahi hua"); }
        });
    }

    void startDownload(String url) {
        try {
            DownloadManager.Request rq = new DownloadManager.Request(Uri.parse(url));
            String name = URLUtil.guessFileName(url, null, null);
            rq.setTitle(name);
            String ck = CookieManager.getInstance().getCookie(url);
            if (ck != null) rq.addRequestHeader("Cookie", ck);
            rq.addRequestHeader("User-Agent", cur.wv.getSettings().getUserAgentString());
            if (cur.wv.getUrl() != null) rq.addRequestHeader("Referer", cur.wv.getUrl());
            rq.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            rq.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(rq);
            toast("Downloading: " + name);
        } catch (Exception e) { toast("Download start nahi hua"); }
    }

    boolean handleInternal(String u) {
        if (u == null || !u.startsWith("mt://")) return false;
        String m = u.substring(5);
        if (m.startsWith("bookmarks")) openLib("bookmarks");
        else if (m.startsWith("history")) openLib("history");
        else if (m.startsWith("downloads")) openLib("downloads");
        else if (m.startsWith("settings")) openSettings();
        return true;
    }

    void openLib(String mode) {
        android.content.Intent i = new android.content.Intent(this, LibraryActivity.class);
        i.putExtra("mode", mode);
        startActivityForResult(i, 77);
    }

    @Override
    protected void onActivityResult(int rc, int res, android.content.Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc == 77 && res == RESULT_OK && d != null && d.getStringExtra("url") != null)
            cur.wv.loadUrl(d.getStringExtra("url"));
    }

    void addBookmark() {
        String u = cur.wv.getUrl();
        if (u == null || u.startsWith("file:")) {
            Toast.makeText(this, "Pehle koi website kholo", Toast.LENGTH_SHORT).show();
            return;
        }
        Store.addBookmark(this, cur.title, u);
        Toast.makeText(this, "Bookmark save ho gaya", Toast.LENGTH_SHORT).show();
    }
}
