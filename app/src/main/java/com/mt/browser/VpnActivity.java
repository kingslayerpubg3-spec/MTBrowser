package com.mt.browser;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VpnActivity extends AppCompatActivity {
    final Handler h = new Handler(Looper.getMainLooper());
    LinearLayout list;
    ImageView power;
    TextView st, sub1, sub2;
    EditText search;
    String status = "";
    int uiVer;
    final Runnable tick = new Runnable() { public void run() { refreshCard(); h.postDelayed(this, 1000); } };

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

    LinearLayout.LayoutParams lp(int w, int hh, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, hh);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        uiVer = UiTheme.ver(this);
        UiTheme.apply(this);
        final int tx = UiTheme.tx(this), sub = UiTheme.sub(this), acc = UiTheme.acc(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{UiTheme.bg1(this), UiTheme.bg2(this)}));

        LinearLayout hr = new LinearLayout(this);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        hr.setPadding(dp(8), dp(24), dp(14), dp(8));
        ImageView bk = new ImageView(this);
        bk.setImageResource(R.drawable.ic_back);
        bk.setColorFilter(tx);
        bk.setPadding(dp(12), dp(12), dp(12), dp(12));
        bk.setOnClickListener(v -> finish());
        hr.addView(bk, new LinearLayout.LayoutParams(dp(48), dp(48)));
        hr.addView(tv("MT VPN", 26, tx, true), lp(-2, -2, 4, 0, 0, 0));
        root.addView(hr);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(4), dp(16), dp(28));
        sv.addView(c);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(18), dp(24), dp(18), dp(22));
        card.setBackground(bg(UiTheme.card(this), UiTheme.stroke(this), 28));
        power = new ImageView(this);
        power.setImageResource(R.drawable.ic_m_power);
        power.setPadding(dp(34), dp(34), dp(34), dp(34));
        power.setOnClickListener(v -> onPower());
        MenuSheet.press(power);
        card.addView(power, new LinearLayout.LayoutParams(dp(120), dp(120)));
        st = tv("", 19, tx, true);
        st.setGravity(Gravity.CENTER);
        card.addView(st, lp(-1, -2, 0, 16, 0, 0));
        sub1 = tv("", 14, sub, false);
        sub1.setGravity(Gravity.CENTER);
        card.addView(sub1, lp(-1, -2, 0, 6, 0, 0));
        sub2 = tv("", 13, sub, false);
        sub2.setGravity(Gravity.CENTER);
        card.addView(sub2, lp(-1, -2, 0, 4, 0, 0));
        c.addView(card, lp(-1, -2, 0, 0, 0, 10));

        TextView warn = tv("Free public servers  \u2022  sirf browser, downloads aur player me kaam karta hai (poore phone me nahi)  \u2022  sirf https sites par use karo  \u2022  bank ya login ke liye mat use karo", 12, sub, false);
        warn.setPadding(dp(6), dp(4), dp(6), dp(10));
        c.addView(warn);

        TextView own = tv("+  Apna server jodo", 15, acc, true);
        own.setGravity(Gravity.CENTER);
        own.setBackground(bg(0, acc, 24));
        own.setPadding(0, dp(13), 0, dp(13));
        own.setOnClickListener(v -> addCustom());
        MenuSheet.press(own);
        c.addView(own, lp(-1, -2, 0, 0, 0, 12));

        search = new EditText(this);
        search.setHint("Country dhundho...");
        search.setHintTextColor(sub);
        search.setTextColor(tx);
        search.setTextSize(15);
        search.setSingleLine(true);
        search.setBackground(bg(UiTheme.card(this), UiTheme.stroke(this), 24));
        search.setPadding(dp(20), 0, dp(20), 0);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c2) { }
            public void onTextChanged(CharSequence s, int a, int b2, int c2) { buildList(); }
            public void afterTextChanged(Editable e) { }
        });
        c.addView(search, lp(-1, dp(48), 0, 0, 0, 12));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        c.addView(list, lp(-1, -2, 0, 0, 0, 0));
        setContentView(root);
        refreshCard();
        buildList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uiVer != UiTheme.ver(this)) { recreate(); return; }
        h.post(tick);
    }

    @Override
    protected void onPause() {
        h.removeCallbacks(tick);
        super.onPause();
    }

    String fmtTime(long s) {
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    void refreshCard() {
        boolean on = VpnManager.connected, bz = VpnManager.busy;
        int[] cols = on ? new int[]{0xFF34D399, 0xFF059669}
                : bz ? new int[]{0xFFF59E0B, 0xFFD97706}
                : new int[]{UiTheme.acc(this), UiTheme.acc2(this)};
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, cols);
        g.setShape(GradientDrawable.OVAL);
        power.setBackground(g);
        power.setColorFilter(Color.WHITE);
        if (on) {
            String code = VpnManager.cc;
            st.setText("Connected  \u2022  " + VpnManager.flag(code) + " " + VpnManager.name(code));
            sub1.setText(VpnManager.host + ":" + VpnManager.port + (VpnManager.ms > 0 ? "  \u2022  " + VpnManager.ms + " ms" : ""));
            sub2.setText(fmtTime((System.currentTimeMillis() - VpnManager.since) / 1000)
                    + (VpnManager.ip.isEmpty() ? "" : "  \u2022  IP " + VpnManager.ip));
        } else if (bz) {
            st.setText(status.isEmpty() ? "Connect ho raha hai..." : status);
            sub1.setText("Rokne ke liye button dabao");
            sub2.setText("");
        } else {
            st.setText("Connected nahi");
            sub1.setText("Neeche se country chuno");
            sub2.setText("");
        }
    }

    void buildList() {
        list.removeAllViews();
        String q = search.getText().toString().trim().toLowerCase();
        int shown = 0;
        for (String[] ct : VpnManager.COUNTRIES) {
            if (!q.isEmpty() && !ct[1].toLowerCase().contains(q) && !ct[0].toLowerCase().contains(q)) continue;
            list.addView(row(ct[0], ct[1]), lp(-1, -2, 0, 0, 0, 8));
            shown++;
        }
        if (shown == 0) list.addView(tv("Koi country nahi mili", 15, UiTheme.sub(this), false));
    }

    View row(final String code, String nm) {
        boolean cur = VpnManager.connected && code.equals(VpnManager.cc);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(14), dp(12), dp(14), dp(12));
        r.setBackground(bg(UiTheme.card(this), cur ? 0xFF22C55E : UiTheme.stroke(this), 18));
        r.addView(tv(VpnManager.flag(code), 26, Color.WHITE, false), new LinearLayout.LayoutParams(dp(42), -2));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.addView(tv(nm, 16, UiTheme.tx(this), true));
        mid.addView(tv(code, 12, UiTheme.sub(this), false));
        r.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
        if (cur) r.addView(tv("\u25CF Connected", 12, 0xFF22C55E, true));
        else {
            ImageView ar = new ImageView(this);
            ar.setImageResource(R.drawable.ic_nav_fwd);
            ar.setColorFilter(UiTheme.sub(this));
            r.addView(ar, new LinearLayout.LayoutParams(dp(20), dp(20)));
        }
        r.setOnClickListener(v -> pick(code));
        MenuSheet.press(r);
        return r;
    }

    final VpnManager.Cb cb = new VpnManager.Cb() {
        public void progress(String s) { status = s; refreshCard(); }
        public void done(boolean ok, String msg) {
            refreshCard();
            buildList();
            if (!ok || !"Connected".equals(msg)) Toast.makeText(VpnActivity.this, msg, Toast.LENGTH_LONG).show();
        }
    };

    void onPower() {
        if (VpnManager.busy) { VpnManager.cancel = true; status = "Cancel ho raha hai..."; refreshCard(); return; }
        if (VpnManager.connected) { VpnManager.disconnect(); refreshCard(); buildList(); return; }
        Toast.makeText(this, "Neeche se country chuno", Toast.LENGTH_SHORT).show();
    }

    void pick(String code) {
        if (VpnManager.busy) { Toast.makeText(this, "Ruko, pehla connect ho raha hai", Toast.LENGTH_SHORT).show(); return; }
        if (VpnManager.connected) VpnManager.disconnect();
        status = "Servers dhundh raha hai...";
        VpnManager.connectCountry(this, code, cb);
        refreshCard();
    }

    void addCustom() {
        if (VpnManager.busy) return;
        final EditText e = new EditText(this);
        e.setHint("host:port  ya  user:pass@host:port");
        e.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("Apna server jodo")
            .setMessage("HTTP ya HTTPS proxy ka address. Apna VPS ya kharida hua proxy use karna sabse safe hai.")
            .setView(e)
            .setPositiveButton("Connect", (d, w) -> {
                String s = e.getText().toString().trim();
                if (s.isEmpty()) return;
                if (VpnManager.connected) VpnManager.disconnect();
                status = "Server check ho raha hai...";
                VpnManager.connectCustom(this, s, cb);
                refreshCard();
            })
            .setNegativeButton("Cancel", null).show();
    }
}
