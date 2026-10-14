package com.mt.browser;

import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import static com.mt.browser.DownloadEngine.ACTIVE;
import static com.mt.browser.DownloadEngine.PAUSED;
import static com.mt.browser.DownloadEngine.DONE;
import static com.mt.browser.DownloadEngine.FAILED;

public class DownloadsActivity extends AppCompatActivity {
    static class Holder { TextView sub; ProgressBar bar; }

    final Handler h = new Handler(Looper.getMainLooper());
    final HashMap<Long, Holder> holders = new HashMap<>();
    final ArrayList<TextView> chips = new ArrayList<>();
    final String[] FN = {"All", "Active", "Done", "Failed"};
    int filter = 0;
    String sig = "";
    LinearLayout list;
    ScrollView sv;
    TextView tvSpeed, tvTask, tvTotal;
    EditText search;
    final Runnable tick = new Runnable() { public void run() { render(false); h.postDelayed(this, 1000); } };

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    GradientDrawable bg(int fill, int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
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

    public static String speedText(double b) {
        if (b < 1024) return "0 KB/s";
        if (b < 1048576) return String.format(Locale.US, "%.0f KB/s", b / 1024);
        return String.format(Locale.US, "%.1f MB/s", b / 1048576);
    }

    static String size(long b) {
        if (b <= 0) return "0 KB";
        if (b < 1048576) return (b / 1024) + " KB";
        if (b < 1073741824L) return String.format(Locale.US, "%.1f MB", b / 1048576.0);
        return String.format(Locale.US, "%.2f GB", b / 1073741824.0);
    }

    static String eta(long left, double sp) {
        if (sp < 1) return "";
        long s = (long) (left / sp);
        if (s < 60) return s + "s left";
        if (s < 3600) return (s / 60) + "m " + (s % 60) + "s left";
        return (s / 3600) + "h " + ((s % 3600) / 60) + "m left";
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        DownloadEngine.init(this);
        filter = getSharedPreferences("mt", 0).getInt("dl_filter", 0);
        getWindow().setStatusBarColor(0xFF0F1A4A);
        getWindow().setNavigationBarColor(0xFF0F1535);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 5);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF0F1A4A, 0xFF150F36}));

        sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(22), dp(18), dp(24));
        sv.addView(c);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout hr = new LinearLayout(this); hr.setGravity(Gravity.CENTER_VERTICAL); ImageView bk = new ImageView(this); bk.setImageResource(R.drawable.ic_back); bk.setPadding(dp(10), dp(10), dp(10), dp(10)); bk.setOnClickListener(v -> finish()); hr.addView(bk, new LinearLayout.LayoutParams(dp(46), dp(46))); hr.addView(tv("Downloads", 30, Color.WHITE, true), lp(-2, -2, 4, 0, 0, 0)); c.addView(hr);
        tvTotal = tv("0 total", 15, 0xFF9AA9CC, false);
        c.addView(tvTotal, lp(-2, -2, 54, 0, 0, 14));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(16), dp(22), dp(16), dp(22));
        GradientDrawable cg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF3B82F6, 0xFF2563EB});
        cg.setCornerRadius(dp(34));
        card.setBackground(cg);
        tvSpeed = tv("0 KB/s", 44, Color.WHITE, true);
        tvSpeed.setGravity(Gravity.CENTER);
        tvSpeed.setGravity(Gravity.CENTER);
        tvTask = tv("0 Tasks", 15, Color.WHITE, false);
        tvTask.setBackground(bg(0x33FFFFFF, 0, 22));
        tvTask.setPadding(dp(20), dp(7), dp(20), dp(7));
        card.addView(tvSpeed, new LinearLayout.LayoutParams(-1, -2));
        card.addView(tvTask, lp(-2, -2, 0, 8, 0, 0));
        c.addView(card, lp(-1, -2, 0, 0, 0, 16));

        search = new EditText(this);
        search.setHint("Search downloads...");
        search.setHintTextColor(0xFF7C8AB0);
        search.setTextColor(Color.WHITE);
        search.setTextSize(16);
        search.setSingleLine(true);
        search.setBackground(bg(0xFF0F1730, 0xFF232F55, 28));
        search.setPadding(dp(22), 0, dp(22), 0);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c2) { }
            public void onTextChanged(CharSequence s, int a, int b2, int c2) { render(true); }
            public void afterTextChanged(Editable e) { }
        });
        c.addView(search, lp(-1, dp(52), 0, 0, 0, 14));

        LinearLayout cr = new LinearLayout(this);
        for (int i = 0; i < FN.length; i++) {
            final int fi = i;
            TextView ch = tv(FN[i], 15, Color.WHITE, true);
            ch.setPadding(dp(20), dp(11), dp(20), dp(11));
            ch.setOnClickListener(v -> { filter = fi; paintChips(); render(true); });
            chips.add(ch);
            cr.addView(ch, lp(-2, -2, 0, 0, 10, 0));
        }
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(cr);
        c.addView(hs, lp(-1, -2, 0, 0, 0, 14));
        paintChips();

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        c.addView(list, lp(-1, -2, 0, 0, 0, 0));

        TextView clr = tv("Clear finished", 17, Color.WHITE, true);
        clr.setGravity(Gravity.CENTER);
        clr.setBackground(bg(0xFF1B2547, 0xFF232F55, 28));
        clr.setPadding(0, dp(18), 0, dp(18));
        clr.setOnClickListener(v -> { DownloadEngine.clearFinished(); render(true); });
        c.addView(clr, lp(-1, -2, 0, 4, 0, 0));

        setContentView(root);
        render(true);
    }

    void paintChips() {
        for (int i = 0; i < chips.size(); i++) {
            TextView t = chips.get(i);
            boolean on = i == filter;
            t.setTextColor(on ? Color.WHITE : 0xFFC5D3FF);
            t.setBackground(bg(on ? 0xFF3B82F6 : 0xFF1B2547, 0, 24));
        }
    }

    LinearLayout nav() {
        LinearLayout n = new LinearLayout(this);
        n.setBackgroundColor(0xFF0F1535);
        n.setPadding(0, dp(8), 0, dp(8));
        int[] ic = {R.drawable.ic_home, R.drawable.ic_m_download, R.drawable.ic_m_folder, R.drawable.ic_m_history, R.drawable.ic_m_settings};
        String[] lb = {"Home", "Downloads", "Files", "History", "Settings"};
        for (int i = 0; i < 5; i++) {
            final int k = i;
            boolean on = i == 1;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            FrameLayout pill = new FrameLayout(this);
            if (on) pill.setBackground(bg(0xFF3B82F6, 0, 18));
            ImageView iv = new ImageView(this);
            iv.setImageResource(ic[i]);
            iv.setColorFilter(on ? Color.WHITE : 0xFF9AA9CC);
            pill.addView(iv, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
            cell.addView(pill, new LinearLayout.LayoutParams(dp(66), dp(36)));
            cell.addView(tv(lb[i], 12, on ? 0xFF4C8DFF : 0xFF9AA9CC, false), lp(-2, -2, 0, 3, 0, 0));
            cell.setOnClickListener(v -> go(k));
            n.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        return n;
    }

    void go(int k) {
        if (k == 0) {
            if (isTaskRoot()) startActivity(new Intent(this, MainActivity.class));
            finish();
        } else if (k == 2) {
            try { startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)); }
            catch (Exception e) { Toast.makeText(this, "Files app nahi khula", Toast.LENGTH_SHORT).show(); }
        } else if (k == 3) {
            startActivityForResult(new Intent(this, LibraryActivity.class).putExtra("mode", "history"), 1);
        } else if (k == 4) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc == 1 && res == RESULT_OK && d != null) { setResult(RESULT_OK, d); finish(); }
    }

    @Override protected void onResume() { super.onResume(); getSharedPreferences("mt", 0).edit().putString("screen", "downloads").apply(); h.post(tick); }
    @Override protected void onPause() { h.removeCallbacks(tick); getSharedPreferences("mt", 0).edit().putInt("dl_filter", filter).apply(); super.onPause(); }

    void render(boolean force) {
        ArrayList<DownloadEngine.Item> all = DownloadEngine.snapshot();
        double sp = 0;
        int act = 0;
        for (DownloadEngine.Item it : all) if (it.running) { act++; sp += it.speed; }
        tvSpeed.setText(speedText(sp));
        tvTask.setText(act + (act == 1 ? " Task" : " Tasks"));
        tvTotal.setText(all.size() + " total");
        String q = search.getText().toString().trim().toLowerCase();
        ArrayList<DownloadEngine.Item> vis = new ArrayList<>();
        for (DownloadEngine.Item it : all) {
            if (!q.isEmpty() && !it.name.toLowerCase().contains(q)) continue;
            int s = it.status;
            boolean ok = filter == 0 || (filter == 1 && (s == ACTIVE || s == PAUSED))
                    || (filter == 2 && s == DONE) || (filter == 3 && s == FAILED);
            if (ok) vis.add(it);
        }
        StringBuilder sb = new StringBuilder(filter + "|" + q + "|");
        for (DownloadEngine.Item it : vis) sb.append(it.id).append(':').append(it.status).append(',');
        if (force || !sb.toString().equals(sig)) { sig = sb.toString(); rebuild(vis); return; }
        for (DownloadEngine.Item it : vis) update(it, holders.get(it.id));
    }

    void rebuild(ArrayList<DownloadEngine.Item> vis) {
        final int sy = sv.getScrollY();
        sv.post(() -> sv.scrollTo(0, sy));
        list.removeAllViews();
        holders.clear();
        if (vis.isEmpty()) {
            TextView e = tv("Koi download nahi hai", 16, 0xFF7C8AB0, false);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, dp(30), 0, dp(30));
            list.addView(e);
            return;
        }
        for (DownloadEngine.Item it : vis) list.addView(card(it), lp(-1, -2, 0, 0, 0, 14));
    }

    TextView btn(String t, int color, View.OnClickListener cl) {
        TextView b = tv(t, 14, color, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(bg(0xFF111A3A, 0xFF2B3A6E, 20));
        b.setOnClickListener(cl);
        return b;
    }

    int iconFor(String n) {
        String e = n.toLowerCase();
        if (e.matches(".*\\.(mp4|mkv|webm|3gp|avi|mov)$")) return R.drawable.ic_m_play;
        if (e.matches(".*\\.(mp3|m4a|aac|wav|ogg|flac)$")) return R.drawable.ic_m_music;
        if (e.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) return R.drawable.ic_m_image;
        return R.drawable.ic_m_file;
    }

    View card(final DownloadEngine.Item it) {
        Holder hd = new Holder();
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(bg(0xFF151D3E, 0xFF232F55, 28));
        row.setPadding(dp(16), dp(16), dp(14), dp(16));
        int s = it.status;
        int col = s == DONE ? 0xFF22C55E : s == FAILED ? 0xFFEF4444 : s == PAUSED ? 0xFFF59E0B : 0xFF3B82F6;
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconFor(it.name));
        icon.setColorFilter(Color.WHITE);
        icon.setPadding(dp(16), dp(16), dp(16), dp(16));
        icon.setBackground(bg(col, 0, 18));
        row.addView(icon, new LinearLayout.LayoutParams(dp(62), dp(62)));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView title = tv(it.name, 16, Color.WHITE, true);
        title.setMaxLines(3);
        title.setEllipsize(TextUtils.TruncateAt.END);
        mid.addView(title);
        if (s != DONE) {
            hd.bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            hd.bar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.dl_progress));
            hd.bar.setMax(1000);
            mid.addView(hd.bar, lp(-1, dp(6), 0, 10, 0, 0));
        }
        hd.sub = tv("", 13, 0xFF9AA9CC, false);
        mid.addView(hd.sub, lp(-2, -2, 0, 8, 0, 0));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -2, 1f);
        mp.setMargins(dp(14), 0, dp(10), 0);
        row.addView(mid, mp);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.VERTICAL);
        String a1 = s == ACTIVE ? "Pause" : s == PAUSED ? "Resume" : s == FAILED ? "Retry" : "Open";
        String a2 = (s == DONE || s == FAILED) ? "Delete" : "Cancel";
        btns.addView(btn(a1, 0xFF6C8CFF, v -> primary(it)), lp(dp(92), dp(40), 0, 0, 0, 8));
        btns.addView(btn(a2, 0xFFFF6B6B, v -> secondary(it)), lp(dp(92), dp(40), 0, 0, 0, 0));
        row.addView(btns);
        if (s == DONE) row.setOnClickListener(v -> open(it));
        holders.put(it.id, hd);
        update(it, hd);
        return row;
    }

    void primary(DownloadEngine.Item it) {
        if (it.status == ACTIVE) DownloadEngine.pause(it);
        else if (it.status == PAUSED || it.status == FAILED) DownloadEngine.resume(it);
        else open(it);
        render(true);
    }

    void secondary(final DownloadEngine.Item it) {
        if (it.status == DONE) {
            new AlertDialog.Builder(this).setTitle(it.name)
                .setItems(new String[]{"Remove from list", "Delete file too"},
                    (d, i) -> { DownloadEngine.remove(it, i == 1); render(true); }).show();
        } else {
            DownloadEngine.remove(it, false);
            render(true);
        }
    }

    void open(DownloadEngine.Item it) {
        try {
            if (it.uri.startsWith("file:")) {
                Toast.makeText(this, "Saved: " + it.name, Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(android.net.Uri.parse(it.uri), DownloadEngine.mime(it.name));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Is file ko kholne wala app nahi mila", Toast.LENGTH_SHORT).show();
        }
    }

    void update(DownloadEngine.Item it, Holder hd) {
        if (hd == null) return;
        int s = it.status;
        if (hd.bar != null) {
            if (it.total > 0) { hd.bar.setIndeterminate(false); animTo(hd.bar, (int) (it.done * 1000 / it.total)); }
            else hd.bar.setIndeterminate(s == ACTIVE);
        }
        String t;
        if (s == ACTIVE) {
            t = speedText(it.speed) + "   " + size(it.done) + (it.total > 0 ? " / " + size(it.total) : "");
            if (it.total > 0) {
                String e = eta(it.total - it.done, it.speed);
                t += "\n" + (it.done * 100 / it.total) + "%" + (e.isEmpty() ? "" : " • " + e);
            }
        } else if (s == PAUSED) {
            t = "Paused • " + size(it.done) + (it.total > 0 ? " / " + size(it.total) : "");
        } else if (s == FAILED) {
            t = "Failed • " + it.err;
        } else {
            t = "Completed • " + size(it.total);
        }
        hd.sub.setText(t);
    }

    void animTo(ProgressBar b, int v) {
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofInt(b, "progress", b.getProgress(), v);
        a.setDuration(900);
        a.setInterpolator(new android.view.animation.LinearInterpolator());
        a.start();
    }

    @Override
    public void finish() {
        getSharedPreferences("mt", 0).edit().putString("screen", "browser").apply();
        super.finish();
    }
}
