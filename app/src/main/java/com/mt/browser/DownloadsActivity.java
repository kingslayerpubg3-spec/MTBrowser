package com.mt.browser;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.mt.browser.DownloadEngine.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import static com.mt.browser.DownloadEngine.ACTIVE;
import static com.mt.browser.DownloadEngine.PAUSED;
import static com.mt.browser.DownloadEngine.DONE;
import static com.mt.browser.DownloadEngine.FAILED;

public class DownloadsActivity extends AppCompatActivity {
    static class Holder { TextView sz, sub, right; ProgressBar bar; ImageView act; }

    final Handler h = new Handler(Looper.getMainLooper());
    final HashMap<Long, Holder> holders = new HashMap<>();
    int uiVer;
    int lastTx, lastTy;
    String sig = "";
    LinearLayout list;
    ScrollView sv;
    TextView tvSpeed, tvTask;
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

    View quick(int icon, int c1, int c2, String label, View.OnClickListener cl) {
        LinearLayout q = new LinearLayout(this);
        q.setGravity(Gravity.CENTER_VERTICAL);
        q.setBackground(bg(UiTheme.chip(this), 0, 18));
        q.setPadding(dp(10), dp(9), dp(10), dp(9));
        ImageView ic = new ImageView(this);
        ic.setImageResource(icon);
        ic.setColorFilter(Color.WHITE);
        ic.setPadding(dp(7), dp(7), dp(7), dp(7));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        g.setShape(GradientDrawable.OVAL);
        ic.setBackground(g);
        q.addView(ic, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView t = tv(label, 13, UiTheme.tx(this), true);
        t.setSingleLine(true);
        q.addView(t, lp(-2, -2, 8, 0, 0, 0));
        q.setOnClickListener(cl);
        return q;
    }

    LinearLayout nav() {
        LinearLayout n = new LinearLayout(this);
        n.setBackgroundColor(UiTheme.card(this));
        n.setPadding(0, dp(8), 0, dp(8));
        int[] ic = {R.drawable.ic_m_download, R.drawable.ic_m_folder};
        String[] lb = {"Download", "Files"};
        for (int i = 0; i < 2; i++) {
            final int k = i;
            int col = i == 0 ? UiTheme.acc(this) : UiTheme.sub(this);
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            ImageView iv = new ImageView(this);
            iv.setImageResource(ic[i]);
            iv.setColorFilter(col);
            cell.addView(iv, new LinearLayout.LayoutParams(dp(26), dp(26)));
            cell.addView(tv(lb[i], 12, col, true), lp(-2, -2, 0, 3, 0, 0));
            cell.setOnClickListener(v -> {
                if (k == 1) {
                    try { startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)); }
                    catch (Exception e) { Toast.makeText(this, "Files app nahi khula", Toast.LENGTH_SHORT).show(); }
                }
            });
            n.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        return n;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        DownloadEngine.init(this);
        uiVer = UiTheme.ver(this);
        UiTheme.apply(this);
        final int acc = UiTheme.acc(this), acc2 = UiTheme.acc2(this);
        getWindow().setStatusBarColor(acc);
        getWindow().setNavigationBarColor(UiTheme.card(this));
        getWindow().getDecorView().setSystemUiVisibility(
                UiTheme.light(this) && Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 5);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiTheme.bg2(this));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{acc, acc2}));
        head.setPadding(dp(8), dp(6), dp(8), dp(20));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView bk = new ImageView(this);
        bk.setImageResource(R.drawable.ic_back);
        bk.setColorFilter(Color.WHITE);
        bk.setPadding(dp(11), dp(11), dp(11), dp(11));
        bk.setOnClickListener(v -> finish());
        top.addView(bk, new LinearLayout.LayoutParams(dp(46), dp(46)));
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        head.addView(top);
        tvSpeed = tv("0 KB/s", 50, Color.WHITE, true);
        tvSpeed.setGravity(Gravity.CENTER);
        head.addView(tvSpeed, new LinearLayout.LayoutParams(-1, -2));
        tvTask = tv("0 Task", 15, Color.WHITE, false);
        tvTask.setBackground(bg(0x33FFFFFF, 0, 20));
        tvTask.setPadding(dp(22), dp(6), dp(22), dp(6));
        LinearLayout.LayoutParams tp = lp(-2, -2, 0, 6, 0, 0);
        tp.gravity = Gravity.CENTER_HORIZONTAL;
        head.addView(tvTask, tp);
        root.addView(head, new LinearLayout.LayoutParams(-1, -2));

        sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        GradientDrawable sg = new GradientDrawable();
        sg.setColor(UiTheme.bg2(this));
        float r = dp(28);
        sg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sv.setBackground(sg);
        sv.setClipToOutline(true);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(18), dp(16), dp(20));
        sv.addView(c);
        LinearLayout body = new LinearLayout(this); body.setBackgroundColor(acc2); body.addView(sv, new LinearLayout.LayoutParams(-1, -1)); LinearLayout.LayoutParams sp2 = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(body, sp2);

        LinearLayout cr = new LinearLayout(this);
        LinearLayout.LayoutParams w1 = lp(0, -2, 0, 0, 6, 0);
        w1.weight = 1f;
        LinearLayout.LayoutParams w2 = lp(0, -2, 6, 0, 0, 0);
        w2.weight = 1f;
        cr.addView(quick(R.drawable.ic_m_star, 0xFFB085FF, 0xFF7545F0, "Watch Later",
                v -> startActivityForResult(new Intent(this, LibraryActivity.class).putExtra("mode", "watch"), 1)), w1);
        cr.addView(quick(R.drawable.ic_m_delete, 0xFFFFB05A, 0xFFFF6F2C, "Clear finished",
                v -> { DownloadEngine.clearFinished(); render(true); }), w2);
        c.addView(cr, lp(-1, -2, 0, 0, 0, 6));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        c.addView(list, lp(-1, -2, 0, 0, 0, 0));

        root.addView(nav(), new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        render(true);
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc == 1 && res == RESULT_OK && d != null) { setResult(RESULT_OK, d); finish(); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uiVer != UiTheme.ver(this)) { recreate(); return; }
        getSharedPreferences("mt", 0).edit().putString("screen", "downloads").apply();
        h.post(tick);
        autoFix();
    }

    @Override protected void onPause() { h.removeCallbacks(tick); super.onPause(); }

    @Override
    public void finish() {
        getSharedPreferences("mt", 0).edit().putString("screen", "browser").apply();
        super.finish();
    }

    void render(boolean force) {
        ArrayList<Item> all = DownloadEngine.snapshot();
        double sp = 0;
        int act = 0;
        ArrayList<Item> a1 = new ArrayList<>(), a2 = new ArrayList<>();
        for (Item it : all) {
            if (it.running) { act++; sp += it.speed; }
            if (it.status == DONE) a2.add(it); else a1.add(it);
        }
        tvSpeed.setText(speedText(sp));
        tvTask.setText(act + (act == 1 ? " Task" : " Tasks"));
        StringBuilder sb = new StringBuilder();
        for (Item it : all) sb.append(it.id).append(':').append(it.status).append(',');
        if (force || !sb.toString().equals(sig)) { sig = sb.toString(); rebuild(a1, a2); return; }
        for (Item it : all) update(it, holders.get(it.id));
    }

    View section(String title, int n) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.addView(tv(title, 17, UiTheme.sub(this), true));
        TextView p = tv(String.valueOf(n), 13, UiTheme.tx(this), true);
        p.setBackground(bg(UiTheme.chip(this), 0, 12));
        p.setPadding(dp(10), dp(2), dp(10), dp(2));
        r.addView(p, lp(-2, -2, 8, 0, 0, 0));
        return r;
    }

    void rebuild(ArrayList<Item> a1, ArrayList<Item> a2) {
        final int sy = sv.getScrollY();
        list.removeAllViews();
        holders.clear();
        if (a1.isEmpty() && a2.isEmpty()) {
            LinearLayout e = new LinearLayout(this);
            e.setOrientation(LinearLayout.VERTICAL);
            e.setGravity(Gravity.CENTER_HORIZONTAL);
            e.setPadding(0, dp(50), 0, 0);
            ImageView ic = new ImageView(this);
            ic.setImageResource(R.drawable.ic_m_download);
            ic.setColorFilter(Color.WHITE);
            ic.setPadding(dp(22), dp(22), dp(22), dp(22));
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(UiTheme.acc(this));
            ic.setBackground(g);
            e.addView(ic, new LinearLayout.LayoutParams(dp(84), dp(84)));
            e.addView(tv("Abhi koi download nahi", 16, UiTheme.sub(this), false), lp(-2, -2, 0, 16, 0, 0));
            list.addView(e);
            return;
        }
        if (!a1.isEmpty()) {
            list.addView(section("Downloading", a1.size()), lp(-1, -2, 4, 14, 0, 4));
            for (Item it : a1) list.addView(row(it));
        }
        if (!a2.isEmpty()) {
            list.addView(section("Downloaded", a2.size()), lp(-1, -2, 4, 18, 0, 4));
            for (Item it : a2) list.addView(row(it));
        }
        sv.post(() -> sv.scrollTo(0, sy));
    }

    int kind(String n) {
        String e = n.toLowerCase();
        if (e.matches(".*\\.(mp4|mkv|webm|3gp|avi|mov|m4v)$")) return 0;
        if (e.matches(".*\\.(mp3|m4a|aac|wav|ogg|flac)$")) return 1;
        if (e.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) return 2;
        return 3;
    }

    View row(final Item it) {
        final Holder hd = new Holder();
        final boolean done = it.status == DONE;
        int tx = UiTheme.tx(this), sub = UiTheme.sub(this);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(12), dp(4), dp(12));
        int k = kind(it.name);
        int col = k == 0 ? 0xFFEF3B2D : k == 1 ? 0xFF8E5CFF : k == 2 ? 0xFF14B8A6 : UiTheme.acc(this);
        ImageView icon = new ImageView(this);
        icon.setImageResource(k == 0 ? R.drawable.ic_m_play : k == 1 ? R.drawable.ic_m_music : k == 2 ? R.drawable.ic_m_image : R.drawable.ic_m_file);
        icon.setColorFilter(Color.WHITE);
        icon.setPadding(dp(12), dp(12), dp(12), dp(12));
        icon.setBackground(bg(col, 0, 14));
        row.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView nm = tv(it.name, 15, tx, false);
        nm.setSingleLine(true);
        nm.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        hd.sz = tv("", 11, sub, false);
        if (!done) {
            LinearLayout t1 = new LinearLayout(this);
            t1.setGravity(Gravity.CENTER_VERTICAL);
            t1.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
            t1.addView(hd.sz, lp(-2, -2, 8, 0, 0, 0));
            mid.addView(t1);
            hd.bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            hd.bar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.dl_progress));
            hd.bar.setProgressTintList(ColorStateList.valueOf(UiTheme.acc(this)));
            hd.bar.setProgressBackgroundTintList(ColorStateList.valueOf(UiTheme.stroke(this)));
            hd.bar.setMax(1000);
            mid.addView(hd.bar, lp(-1, dp(6), 0, 8, 0, 0));
            LinearLayout b2 = new LinearLayout(this);
            hd.sub = tv("", 12, sub, false);
            hd.right = tv("", 11, sub, false);
            b2.addView(hd.sub, new LinearLayout.LayoutParams(0, -2, 1f));
            b2.addView(hd.right);
            mid.addView(b2, lp(-1, -2, 0, 6, 0, 0));
        } else {
            mid.addView(nm);
            mid.addView(hd.sz, lp(-2, -2, 0, 3, 0, 0));
        }
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -2, 1f);
        mp.setMargins(dp(14), 0, dp(12), 0);
        row.addView(mid, mp);

        ImageView act = new ImageView(this);
        act.setImageResource(done ? R.drawable.ic_m_share : it.status == ACTIVE ? R.drawable.ic_m_pause
                : it.status == PAUSED ? R.drawable.ic_m_play : R.drawable.ic_m_refresh);
        act.setColorFilter(tx);
        act.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable ag = new GradientDrawable();
        ag.setShape(GradientDrawable.OVAL);
        ag.setColor(Color.TRANSPARENT);
        ag.setStroke(dp(1), UiTheme.stroke(this));
        act.setBackground(ag);
        act.setOnClickListener(v -> { if (done) share(it); else primary(it); });
        row.addView(act, new LinearLayout.LayoutParams(dp(44), dp(44)));
        hd.act = act;
        row.setOnClickListener(v -> { if (it.status == DONE) open(it); });
        row.setOnTouchListener((v, e) -> { lastTx = (int) e.getRawX(); lastTy = (int) e.getRawY(); return false; });
        row.setOnLongClickListener(v -> { options(it, v); return true; });
        holders.put(it.id, hd);
        update(it, hd);
        return row;
    }

    void primary(Item it) {
        if (it.status == ACTIVE) DownloadEngine.pause(it);
        else if (it.status == PAUSED || it.status == FAILED) DownloadEngine.resume(it);
        render(true);
    }

    void optionsOld(final Item it) {
        if (it.status == DONE) {
            new AlertDialog.Builder(this).setTitle(it.name)
                .setItems(new String[]{"Remove from list", "Delete file too", "Rename", "Fix file type (auto)"},
                    (d, i) -> doOpt(it, i)).show();
        } else {
            new AlertDialog.Builder(this).setTitle(it.name)
                .setItems(new String[]{"Cancel download"}, (d, i) -> { DownloadEngine.remove(it, false); render(true); }).show();
        }
    }

    void share(Item it) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(DownloadEngine.mime(it.name));
            i.putExtra(Intent.EXTRA_STREAM, Uri.parse(it.uri));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Share"));
        } catch (Exception e) {
            Toast.makeText(this, "Share nahi ho saka", Toast.LENGTH_SHORT).show();
        }
    }

    void open(Item it) {
        try {
            if (it.uri.startsWith("file:")) {
                Toast.makeText(this, "Saved: " + it.name, Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse(it.uri), DownloadEngine.mime(it.name));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Is file ko kholne wala app nahi mila", Toast.LENGTH_SHORT).show();
        }
    }

    void animTo(ProgressBar b, int v) {
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofInt(b, "progress", b.getProgress(), v);
        a.setDuration(900);
        a.setInterpolator(new android.view.animation.LinearInterpolator());
        a.start();
    }

    void update(Item it, Holder hd) {
        if (hd == null) return;
        int s = it.status;
        hd.sz.setText(s == DONE ? size(it.total) : (it.total > 0 ? size(it.total) : "?"));
        if (hd.bar != null) {
            if (it.total > 0) { hd.bar.setIndeterminate(false); animTo(hd.bar, (int) (it.done * 1000 / it.total)); }
            else hd.bar.setIndeterminate(s == ACTIVE);
        }
        if (hd.sub != null) {
            String t;
            if (s == ACTIVE) {
                String e = it.total > 0 ? eta(it.total - it.done, it.speed) : "";
                t = !it.note.isEmpty() ? it.note : speedText(it.speed) + (e.isEmpty() ? "" : "  •  " + e);
            } else if (s == PAUSED) t = it.err.isEmpty() ? "Paused" : "Paused • " + it.err;
            else t = "Failed • " + it.err;
            hd.sub.setText(t);
            hd.right.setText(size(it.done));
        }
    }

    void doOpt(final Item it, int i) {
        if (i <= 1) { DownloadEngine.remove(it, i == 1); render(true); return; }
        if (i == 2) {
            final EditText e = new EditText(this);
            e.setText(it.name);
            new AlertDialog.Builder(this).setTitle("Rename").setView(e)
                .setPositiveButton("OK", (d, k) -> {
                    String s = e.getText().toString().trim();
                    if (!s.isEmpty() && !DownloadEngine.rename(it, s))
                        Toast.makeText(this, "Rename nahi hua", Toast.LENGTH_SHORT).show();
                    render(true);
                }).setNegativeButton("Cancel", null).show();
        } else {
            Toast.makeText(this, DownloadEngine.fixExt(it) ? "File type theek ho gaya: " + it.name
                    : "Type pehchan nahi paya, Rename use karo", Toast.LENGTH_LONG).show();
            render(true);
        }
    }

    void autoFix() {
        new Thread(() -> {
            boolean ch = false;
            for (Item it : DownloadEngine.snapshot())
                if (it.status == DONE && DownloadEngine.needsExt(it.name) && DownloadEngine.fixExt(it)) ch = true;
            if (ch) runOnUiThread(() -> render(true));
        }).start();
    }

    void options(final Item it, View anchor) {
        final android.widget.PopupWindow pw = new android.widget.PopupWindow(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(6), dp(8), dp(6), dp(8));
        box.setBackground(bg(UiTheme.card(this), UiTheme.stroke(this), 20));
        ArrayList<String> names = new ArrayList<>();
        final ArrayList<Integer> ids = new ArrayList<>();
        if (it.status == DONE) {
            names.add("Rename"); ids.add(0);
            names.add("Open Folder"); ids.add(1);
            names.add("Re-Download"); ids.add(2);
            names.add("Details"); ids.add(3);
            if (kind(it.name) == 0) { names.add("Check & fix (all players)"); ids.add(7); }
            names.add("Delete"); ids.add(4);
        } else {
            names.add(it.status == ACTIVE ? "Pause" : it.status == PAUSED ? "Resume" : "Retry"); ids.add(5);
            names.add("Details"); ids.add(3);
            names.add("Cancel"); ids.add(6);
        }
        for (int i = 0; i < names.size(); i++) {
            final int act = ids.get(i);
            boolean red = act == 4 || act == 6;
            TextView t = tv(names.get(i), 16, red ? 0xFFEF4444 : UiTheme.tx(this), false);
            t.setPadding(dp(18), dp(13), dp(18), dp(13));
            t.setOnClickListener(v -> { pw.dismiss(); doAct(it, act); });
            box.addView(t, new LinearLayout.LayoutParams(-1, -2));
        }
        pw.setContentView(box);
        pw.setWidth(dp(210));
        pw.setHeight(-2);
        pw.setFocusable(true);
        pw.setOutsideTouchable(true);
        pw.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        pw.setElevation(dp(12));
        box.measure(View.MeasureSpec.makeMeasureSpec(dp(210), View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
        int w = dp(210), hg = box.getMeasuredHeight();
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int x = Math.max(dp(12), Math.min(lastTx - w / 2, dm.widthPixels - w - dp(12)));
        int y = Math.max(dp(40), Math.min(lastTy - hg / 2, dm.heightPixels - hg - dp(48)));
        pw.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
        box.setAlpha(0f);
        box.setScaleX(0.92f);
        box.setScaleY(0.92f);
        box.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).start();
    }

    void doAct(final Item it, int act) {
        switch (act) {
            case 0: doRename(it); break;
            case 1: openFolder(); break;
            case 2:
                DownloadEngine.redownload(this, it);
                Toast.makeText(this, "Download shuru ho gaya", Toast.LENGTH_SHORT).show();
                render(true);
                break;
            case 3: details(it); break;
            case 4: doDelete(it); break;
            case 5: primary(it); break;
            case 6: DownloadEngine.remove(it, false); render(true); break;
            case 7: seekFix(it); break;
        }
    }

    void doRename(final Item it) {
        final EditText e = new EditText(this);
        e.setText(it.name);
        e.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Rename").setView(e)
            .setPositiveButton("OK", (d, k) -> {
                String s = e.getText().toString().trim();
                int dot = it.name.lastIndexOf('.');
                if (!s.isEmpty() && s.indexOf('.') < 0 && dot > 0) s += it.name.substring(dot);
                if (!s.isEmpty() && !DownloadEngine.rename(it, s))
                    Toast.makeText(this, "Rename nahi hua", Toast.LENGTH_SHORT).show();
                render(true);
            }).setNegativeButton("Cancel", null).show();
    }

    void openFolder() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FMT%20Browser"),
                    "vnd.android.document/directory");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try { startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)); }
            catch (Exception x) { Toast.makeText(this, "Files app nahi khula", Toast.LENGTH_SHORT).show(); }
        }
    }

    void details(final Item it) {
        String st = it.status == DONE ? "Completed" : it.status == ACTIVE ? "Downloading" : it.status == PAUSED ? "Paused" : "Failed";
        String dt = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(new java.util.Date(it.id));
        String where = (it.uri != null && it.uri.startsWith("file:")) ? Uri.parse(it.uri).getPath() : "Download/MT Browser";
        String msg = "Name: " + it.name + "\nType: " + DownloadEngine.headerType(it) + "\nSize: " + size(it.total > 0 ? it.total : it.done) + "\nStatus: " + st
                + "\nDate: " + dt + "\nFolder: " + where + "\nSource: " + (it.url == null || it.url.isEmpty() ? "-" : it.url);
        new AlertDialog.Builder(this).setTitle("Details").setMessage(msg)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy link", (d, w) -> {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("MT", it.url));
                Toast.makeText(this, "Link copy ho gaya", Toast.LENGTH_SHORT).show();
            }).show();
    }

    void doDelete(final Item it) {
        new AlertDialog.Builder(this).setTitle("Delete?").setMessage(it.name)
            .setPositiveButton("Delete file", (d, w) -> { DownloadEngine.remove(it, true); render(true); })
            .setNeutralButton("Sirf list se hatao", (d, w) -> { DownloadEngine.remove(it, false); render(true); })
            .setNegativeButton("Cancel", null).show();
    }

    void seekFixOld(final Item it) {
        Toast.makeText(this, "Seekable MP4 ban raha hai... badi file me 1-3 minute lag sakte hain", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            final String r = DownloadEngine.makeSeekable(it);
            runOnUiThread(() -> {
                Toast.makeText(this, r == null ? "Seekable MP4 ban gaya: " + it.name : r, Toast.LENGTH_LONG).show();
                render(true);
            });
        }).start();
    }

    void seekFix(final Item it) {
        Toast.makeText(this, "File check ho rahi hai... badi file me 1-3 minute lag sakte hain", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            final String b = DownloadEngine.diagnose(it);
            final boolean pass = b.contains("Seek test (beech me jump): PASS") && b.contains("Type: MP4");
            String r = null;
            if (!pass) r = DownloadEngine.makeSeekable(it);
            final String after = DownloadEngine.diagnose(it);
            final String res = pass ? "File pehle se theek hai." : (r == null ? "Seekable MP4 ban gaya." : r);
            final String hint = pass
                ? "\n\nFile me seek theek hai. Agar MX Player me phir bhi atakta hai to MX me Settings > Decoder > S/W decoder chuno, ya file MT Player me kholo."
                : (r == null ? "\n\nAb file MP4 hai. Dusre player me dobara try karo." : "\n\nIs file ko MP4 nahi bana sakte (upar wajah likhi hai). Is file ko MX Player ya VLC me hi chalana hoga.");
            runOnUiThread(() -> {
                String msg = "PEHLE:\n" + b + "\nRESULT: " + res + (pass ? "" : "\n\nBAAD ME:\n" + after) + hint;
                new AlertDialog.Builder(this).setTitle("Check & fix").setMessage(msg).setPositiveButton("OK", null).show();
                render(true);
            });
        }).start();
    }
}
