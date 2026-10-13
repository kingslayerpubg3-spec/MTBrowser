package com.mt.browser;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    LinearLayout root;
    TextView engineSub;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    View row(String title, String sub, View right, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        TextView a = new TextView(this);
        a.setText(title);
        a.setTextColor(Color.WHITE);
        a.setTextSize(16);
        t.addView(a);
        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextColor(Color.parseColor("#8A94A8"));
        s.setTextSize(12);
        t.addView(s);
        r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (right != null) r.addView(right);
        if (click != null) r.setOnClickListener(click);
        root.addView(r);
        return s;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#050B1E"));
        root.setPadding(0, dp(24), 0, 0);

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(dp(16), dp(8), dp(16), dp(16));
        root.addView(title);

        final Switch sw = new Switch(this);
        sw.setChecked(Prefs.adBlock(this));
        sw.setOnCheckedChangeListener((v, on) -> Prefs.setAdBlock(this, on));
        row("Ad Blocker", "Ads aur trackers block karo", sw, v -> sw.toggle());

        engineSub = (TextView) row("Default Search Engine", Prefs.engineName(this), null, v ->
            new AlertDialog.Builder(this).setTitle("Search Engine")
                .setItems(Prefs.NAMES, (d, i) -> {
                    Prefs.setEngine(this, i);
                    engineSub.setText(Prefs.engineName(this));
                }).show());

        row("Clear Data", "History, bookmarks, cookies, cache", null, v ->
            new AlertDialog.Builder(this).setTitle("Clear all data?")
                .setPositiveButton("Clear", (d, i) -> {
                    Store.clear(this, "hist");
                    Store.clear(this, "bm");
                    CookieManager.getInstance().removeAllCookies(null);
                    WebStorage.getInstance().deleteAllData();
                    Toast.makeText(this, "Data clear ho gaya", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show());

        row("About MT Browser", "Version 1.0 • Fast, Safe, Smart", null, null);
        setContentView(root);
    }
}
