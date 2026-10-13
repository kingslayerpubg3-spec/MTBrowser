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
            @Override public void onPageStarted(WebView v, String u, Bitmap f) {
                if (t == cur) urlBar.setText(u.startsWith("file:///android_asset") ? "" : u);
            }
            @Override public void onPageFinished(WebView v, String u) {
                String ti = v.getTitle();
                if (ti != null && !ti.isEmpty()) t.title = ti;
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                if (t != cur) return;
                progress.setProgress(p);
                progress.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
            }
        });
        w.setDownloadListener((url, ua, cd, mime, len) -> {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            String name = URLUtil.guessFileName(url, cd, mime);
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
        pm.setOnMenuItemClickListener(i -> {
            switch (i.getItemId()) {
                case 1: newTab(false); break;
                case 2: newTab(true); break;
                case 3: closeTab(cur); break;
                case 4: cur.wv.reload(); break;
            }
            return true;
        });
        pm.show();
    }

    void load(String q) {
        if (q.isEmpty()) return;
        if (q.startsWith("http://") || q.startsWith("https://")) cur.wv.loadUrl(q);
        else if (q.contains(".") && !q.contains(" ")) cur.wv.loadUrl("https://" + q);
        else cur.wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(q));
    }

    @Override
    public void onBackPressed() {
        if (cur.wv.canGoBack()) cur.wv.goBack();
        else if (tabs.size() > 1) closeTab(cur);
        else super.onBackPressed();
    }
}
