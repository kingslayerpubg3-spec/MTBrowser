package com.mt.browser;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

public class LibraryActivity extends AppCompatActivity {
    static final int[] PAL = {0xFF14C9A8, 0xFFFFB300, 0xFF4C8DFF, 0xFF8E5CFF, 0xFFFF8F3A, 0xFFEC4899, 0xFF22C55E, 0xFFEF4444};
    String mode, key;
    LinearLayout list;
    EditText search;
    TextView count;
    int uiVer;

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

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "bookmarks";
        key = mode.equals("history") ? "hist" : "bm";
        uiVer = UiTheme.ver(this);
        UiTheme.apply(this);
        int tx = UiTheme.tx(this), sub = UiTheme.sub(this), acc = UiTheme.acc(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{UiTheme.bg1(this), UiTheme.bg2(this)}));

        LinearLayout hr = new LinearLayout(this);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        hr.setPadding(dp(8), dp(24), dp(14), dp(4));
        ImageView bk = new ImageView(this);
        bk.setImageResource(R.drawable.ic_back);
        bk.setColorFilter(tx);
        bk.setPadding(dp(12), dp(12), dp(12), dp(12));
        bk.setOnClickListener(v -> finish());
        hr.addView(bk, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout tc = new LinearLayout(this);
        tc.setOrientation(LinearLayout.VERTICAL);
        tc.addView(tv(mode.equals("history") ? "History" : "Bookmarks", 26, tx, true));
        count = tv("", 13, sub, false);
        tc.addView(count);
        hr.addView(tc, lp(0, -2, 4, 0, 0, 0));
        ((LinearLayout.LayoutParams) tc.getLayoutParams()).weight = 1f;
        ImageView pl = new ImageView(this);
        pl.setImageResource(R.drawable.ic_m_palette);
        pl.setColorFilter(tx);
        pl.setPadding(dp(10), dp(10), dp(10), dp(10));
        pl.setOnClickListener(v -> ThemePicker.dialog(LibraryActivity.this));
        hr.addView(pl, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView clr = tv("Clear all", 13, acc, true);
        clr.setBackground(bg(UiTheme.chip(this), 0, 18));
        clr.setPadding(dp(14), dp(8), dp(14), dp(8));
        clr.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Sab hata dein?")
                .setPositiveButton("Clear", (d, i) -> { Store.clear(this, key); render(); })
                .setNegativeButton("Cancel", null).show());
        hr.addView(clr);
        root.addView(hr);

        search = new EditText(this);
        search.setHint("Search...");
        search.setHintTextColor(sub);
        search.setTextColor(tx);
        search.setTextSize(15);
        search.setSingleLine(true);
        search.setBackground(bg(UiTheme.card(this), UiTheme.stroke(this), 24));
        search.setPadding(dp(20), 0, dp(20), 0);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c2) { }
            public void onTextChanged(CharSequence s, int a, int b2, int c2) { render(); }
            public void afterTextChanged(Editable e) { }
        });
        root.addView(search, lp(-1, dp(48), 16, 8, 16, 12));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), 0, dp(16), dp(24));
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        render();
    }

    void render() {
        list.removeAllViews();
        JSONArray a = Store.get(this, key);
        String q = search.getText().toString().trim().toLowerCase();
        int shown = 0;
        for (int i = 0; i < a.length(); i++) {
            try {
                JSONObject o = a.getJSONObject(i);
                String t = o.getString("title"), u = o.getString("url");
                if (!q.isEmpty() && !t.toLowerCase().contains(q) && !u.toLowerCase().contains(q)) continue;
                list.addView(card(t, u, i, shown), lp(-1, -2, 0, 0, 0, 10));
                shown++;
            } catch (Exception e) { }
        }
        count.setText(a.length() + (a.length() == 1 ? " item" : " items"));
        if (shown == 0) list.addView(empty());
    }

    View card(String t, final String u, final int idx, int pos) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(bg(UiTheme.card(this), UiTheme.stroke(this), 18));
        row.setPadding(dp(14), dp(12), dp(6), dp(12));
        String host = "";
        try { host = Uri.parse(u).getHost(); } catch (Exception e) { }
        if (host == null) host = "";
        String h2 = host.startsWith("www.") ? host.substring(4) : host;
        TextView ic = tv(h2.isEmpty() ? "•" : h2.substring(0, 1).toUpperCase(), 18, Color.WHITE, true);
        ic.setGravity(Gravity.CENTER);
        GradientDrawable cg = new GradientDrawable();
        cg.setShape(GradientDrawable.OVAL);
        cg.setColor(PAL[(h2.hashCode() & 0x7fffffff) % PAL.length]);
        ic.setBackground(cg);
        row.addView(ic, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView t1 = tv(t, 15, UiTheme.tx(this), true);
        t1.setSingleLine(true);
        t1.setEllipsize(TextUtils.TruncateAt.END);
        TextView t2 = tv(u.replaceFirst("^https?://(www\\.)?", ""), 12, UiTheme.sub(this), false);
        t2.setSingleLine(true);
        t2.setEllipsize(TextUtils.TruncateAt.END);
        mid.addView(t1);
        mid.addView(t2);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -2, 1f);
        mp.setMargins(dp(12), 0, dp(6), 0);
        row.addView(mid, mp);

        ImageView x = new ImageView(this);
        x.setImageResource(R.drawable.ic_m_close);
        x.setColorFilter(UiTheme.sub(this));
        x.setPadding(dp(11), dp(11), dp(11), dp(11));
        x.setOnClickListener(v -> { Store.remove(this, key, idx); render(); });
        row.addView(x, new LinearLayout.LayoutParams(dp(40), dp(40)));

        row.setOnClickListener(v -> {
            Intent r = new Intent();
            r.putExtra("url", u);
            setResult(RESULT_OK, r);
            finish();
        });
        row.setAlpha(0f);
        row.setTranslationY(dp(10));
        row.animate().alpha(1f).translationY(0f).setStartDelay(Math.min(pos, 12) * 25L).setDuration(260).start();
        return row;
    }

    View empty() {
        LinearLayout e = new LinearLayout(this);
        e.setOrientation(LinearLayout.VERTICAL);
        e.setGravity(Gravity.CENTER_HORIZONTAL);
        e.setPadding(0, dp(60), 0, 0);
        ImageView ic = new ImageView(this);
        ic.setImageResource(mode.equals("history") ? R.drawable.ic_m_history : R.drawable.ic_m_bookmark);
        ic.setColorFilter(Color.WHITE);
        ic.setPadding(dp(22), dp(22), dp(22), dp(22));
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(UiTheme.acc(this));
        ic.setBackground(g);
        e.addView(ic, new LinearLayout.LayoutParams(dp(84), dp(84)));
        e.addView(tv(mode.equals("history") ? "Abhi koi history nahi" : "Abhi koi bookmark nahi", 16, UiTheme.sub(this), false), lp(-2, -2, 0, 16, 0, 0));
        return e;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uiVer != UiTheme.ver(this)) recreate();
    }
}
