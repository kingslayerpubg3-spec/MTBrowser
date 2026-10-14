package com.mt.browser;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    LinearLayout c;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    GradientDrawable bg(int fill, int stroke, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(r));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    TextView tv(String t, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    LinearLayout group(String title) {
        TextView s = tv(title.toUpperCase(), 12, UiTheme.sub(this), true);
        s.setLetterSpacing(0.1f);
        c.addView(s, lp(-2, -2, 6, 6, 0, 8));
        LinearLayout g = new LinearLayout(this);
        g.setOrientation(LinearLayout.VERTICAL);
        g.setBackground(bg(UiTheme.card(this), UiTheme.stroke(this), 22));
        c.addView(g, lp(-1, -2, 0, 0, 0, 14));
        return g;
    }

    TextView row(LinearLayout g, int icon, int col, String title, String subt, View right, View.OnClickListener cl) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(14), dp(13), dp(14), dp(13));
        ImageView ic = new ImageView(this);
        ic.setImageResource(icon);
        ic.setColorFilter(Color.WHITE);
        ic.setPadding(dp(9), dp(9), dp(9), dp(9));
        ic.setBackground(bg(col, 0, 12));
        r.addView(ic, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(tv(title, 16, UiTheme.tx(this), true));
        TextView st = tv(subt, 12, UiTheme.sub(this), false);
        t.addView(st);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMargins(dp(14), 0, dp(8), 0);
        r.addView(t, tp);
        if (right != null) r.addView(right);
        if (cl != null) r.setOnClickListener(cl);
        g.addView(r);
        return st;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        UiTheme.apply(this);
        int tx = UiTheme.tx(this), acc = UiTheme.acc(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{UiTheme.bg1(this), UiTheme.bg2(this)}));

        LinearLayout hr = new LinearLayout(this);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        hr.setPadding(dp(8), dp(24), dp(14), dp(8));
        ImageView bk = new ImageView(this);
        bk.setImageResource(R.drawable.ic_back);
        bk.setColorFilter(tx);
        bk.setPadding(dp(12), dp(12), dp(12), dp(12));
        bk.setOnClickListener(v -> finish());
        hr.addView(bk, new LinearLayout.LayoutParams(dp(48), dp(48)));
        hr.addView(tv("Settings", 26, tx, true), lp(-2, -2, 4, 0, 0, 0));
        root.addView(hr);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(4), dp(16), dp(28));
        sv.addView(c);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        LinearLayout g1 = group("Appearance");
        LinearLayout pk = new LinearLayout(this);
        pk.setPadding(dp(14), dp(0), dp(14), dp(16));
        pk.addView(ThemePicker.build(this, () -> recreate()), new LinearLayout.LayoutParams(-1, -2));
        g1.addView(pk);
        final TextView[] bt = new TextView[1];
        bt[0] = row(g1, R.drawable.ic_m_moon, 0xFF6C4DE0, "Browser theme", Prefs.themeName(this), null,
            v -> new AlertDialog.Builder(this).setTitle("Browser theme")
                .setItems(new String[]{"Dark", "Light", "Auto (system)"}, (d, i) -> {
                    Prefs.setTheme(this, i);
                    bt[0].setText(Prefs.themeName(this));
                }).show());

        LinearLayout g2 = group("Browsing");
        final Switch sw = new Switch(this);
        sw.setChecked(Prefs.adBlock(this));
        sw.setThumbTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{acc, 0xFFB0B8CC}));
        sw.setTrackTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{(acc & 0x00FFFFFF) | 0x66000000, 0x33888888}));
        sw.setOnCheckedChangeListener((v, on) -> Prefs.setAdBlock(this, on));
        row(g2, R.drawable.ic_m_shield, 0xFF22C55E, "Ad Blocker", "Ads aur trackers block karo", sw, v -> sw.toggle());
        final TextView[] es = new TextView[1];
        es[0] = row(g2, R.drawable.ic_search, 0xFF3B82F6, "Search engine", Prefs.engineName(this), null,
            v -> new AlertDialog.Builder(this).setTitle("Search engine")
                .setItems(Prefs.NAMES, (d, i) -> {
                    Prefs.setEngine(this, i);
                    es[0].setText(Prefs.engineName(this));
                }).show());

        LinearLayout g3 = group("Data");
        row(g3, R.drawable.ic_m_delete, 0xFFEF4444, "Clear data", "History, bookmarks, cookies, cache", null,
            v -> new AlertDialog.Builder(this).setTitle("Sab data clear karein?")
                .setPositiveButton("Clear", (d, i) -> {
                    Store.clear(this, "hist");
                    Store.clear(this, "bm");
                    CookieManager.getInstance().removeAllCookies(null);
                    WebStorage.getInstance().deleteAllData();
                    Toast.makeText(this, "Data clear ho gaya", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show());
        row(g3, R.drawable.ic_m_info, 0xFF8E5CFF, "About MT Browser", "Version 1.0 • Fast, Safe, Smart", null, null);
    }
}
