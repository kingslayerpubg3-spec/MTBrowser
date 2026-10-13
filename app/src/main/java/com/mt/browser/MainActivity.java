package com.mt.browser;

import android.app.DownloadManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    static final String HOME = "https://www.google.com";
    WebView web;
    EditText urlBar;
    ProgressBar progress;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        web = findViewById(R.id.web);
        urlBar = findViewById(R.id.urlBar);
        progress = findViewById(R.id.progress);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String u, Bitmap f) { urlBar.setText(u); }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                progress.setProgress(p);
                progress.setVisibility(p < 100 ? android.view.View.VISIBLE : android.view.View.GONE);
            }
        });
        web.setDownloadListener((url, ua, cd, mime, len) -> {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            String name = URLUtil.guessFileName(url, cd, mime);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
            Toast.makeText(this, "Downloading: " + name, Toast.LENGTH_SHORT).show();
        });

        urlBar.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_GO || (e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                load(urlBar.getText().toString().trim());
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); });
        findViewById(R.id.btnFwd).setOnClickListener(v -> { if (web.canGoForward()) web.goForward(); });
        findViewById(R.id.btnHome).setOnClickListener(v -> web.loadUrl(HOME));
        findViewById(R.id.btnReload).setOnClickListener(v -> web.reload());

        web.loadUrl(HOME);
    }

    void load(String q) {
        if (q.isEmpty()) return;
        if (q.startsWith("http://") || q.startsWith("https://")) web.loadUrl(q);
        else if (q.contains(".") && !q.contains(" ")) web.loadUrl("https://" + q);
        else web.loadUrl("https://www.google.com/search?q=" + Uri.encode(q));
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
