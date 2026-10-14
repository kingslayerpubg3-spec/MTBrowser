package com.mt.browser;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;

public class MenuSheet {
    static final ArrayList<View> anim = new ArrayList<>();

    static int dp(MainActivity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }
    static LinearLayout.LayoutParams w() { return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); }

    static GradientDrawable grad(int c1, int c2, boolean oval, float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        if (oval) g.setShape(GradientDrawable.OVAL); else g.setCornerRadius(r);
        return g;
    }

    static void press(View v) {
        v.setOnTouchListener((x, e) -> {
            int ac = e.getAction();
            if (ac == MotionEvent.ACTION_DOWN) x.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).start();
            else if (ac == MotionEvent.ACTION_UP || ac == MotionEvent.ACTION_CANCEL) x.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            return false;
        });
    }

    static LinearLayout row(MainActivity a) {
        LinearLayout r = new LinearLayout(a);
        r.setPadding(0, dp(a, 14), 0, 0);
        return r;
    }

    static TextView label(MainActivity a, String t, int tx) {
        TextView v = new TextView(a);
        v.setText(t);
        v.setTextSize(12);
        v.setTextColor(tx);
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(true);
        v.setEllipsize(TextUtils.TruncateAt.END);
        v.setPadding(0, dp(a, 8), 0, 0);
        return v;
    }

    static View circleItem(final MainActivity a, final BottomSheetDialog d, int res, String lb,
                           int c1, int c2, int tx, final Runnable run) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView ic = new ImageView(a);
        ic.setImageResource(res);
        ic.setColorFilter(Color.WHITE);
        int p = dp(a, 16);
        ic.setPadding(p, p, p, p);
        ic.setBackground(grad(c1, c2, true, 0));
        c.addView(ic, new LinearLayout.LayoutParams(dp(a, 56), dp(a, 56)));
        c.addView(label(a, lb, tx));
        c.setOnClickListener(v -> { d.dismiss(); run.run(); });
        press(c);
        anim.add(c);
        return c;
    }

    static View tileItem(final MainActivity a, final BottomSheetDialog d, int res, String lb,
                         int accent, int fill, int stroke, int tx, final Runnable run) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView ic = new ImageView(a);
        ic.setImageResource(res);
        ic.setColorFilter(accent);
        int p = dp(a, 15);
        ic.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(a, 18));
        g.setStroke(dp(a, 1), stroke);
        ic.setBackground(g);
        c.addView(ic, new LinearLayout.LayoutParams(dp(a, 56), dp(a, 56)));
        c.addView(label(a, lb, tx));
        c.setOnClickListener(v -> { d.dismiss(); run.run(); });
        press(c);
        anim.add(c);
        return c;
    }

    static View roundBtn(MainActivity a, int res, int tint, int bgc, View.OnClickListener cl) {
        LinearLayout box = new LinearLayout(a);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, dp(a, 12), 0, dp(a, 4));
        ImageView v = new ImageView(a);
        v.setImageResource(res);
        v.setColorFilter(tint);
        int p = dp(a, 11);
        v.setPadding(p, p, p, p);
        v.setBackground(grad(bgc, bgc, true, 0));
        box.addView(v, new LinearLayout.LayoutParams(dp(a, 46), dp(a, 46)));
        box.setOnClickListener(cl);
        press(box);
        return box;
    }

    static void tools(final MainActivity a) {
        String[] n = {"Translate page", "Find in page", "Copy page text", "Copy link",
                      "Desktop site", "Pop-up video (PiP)", "Smart Download"};
        new AlertDialog.Builder(a).setTitle("Tools").setItems(n, (x, i) -> {
            switch (i) {
                case 0: a.translate(); break;
                case 1: a.findInPage(); break;
                case 2: a.copyText(); break;
                case 3: a.copyLink(); break;
                case 4: a.toggleDesktop(); break;
                case 5: a.enterPip(); break;
                case 6: a.smartDownload(); break;
            }
        }).show();
    }

    static void show(final MainActivity a) {
        anim.clear();
        final BottomSheetDialog d = new BottomSheetDialog(a);
        final boolean l = a.isLight();
        int tx = l ? 0xFF2A2F3A : 0xFFE8EDFF;
        int sub = l ? 0xFF8A93A6 : 0xFF8FA0C0;
        int fill = l ? 0xFFFFFFFF : 0x14FFFFFF;
        int stroke = l ? 0xFFE3E7F0 : 0x1FFFFFFF;

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a, 18), dp(a, 10), dp(a, 18), dp(a, 14));
        GradientDrawable rb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                l ? new int[]{0xFFFFFFFF, 0xFFEAF0FC} : new int[]{0xFF16244F, 0xFF0A112B});
        float cr = dp(a, 28);
        rb.setCornerRadii(new float[]{cr, cr, cr, cr, 0, 0, 0, 0});
        root.setBackground(rb);

        View handle = new View(a);
        handle.setBackground(grad(l ? 0xFFCDD3E0 : 0x44FFFFFF, l ? 0xFFCDD3E0 : 0x44FFFFFF, false, dp(a, 3)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(a, 40), dp(a, 4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = dp(a, 16);
        root.addView(handle, hp);

        LinearLayout h = new LinearLayout(a);
        h.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(a);
        logo.setImageResource(R.drawable.ic_logo_full);
        h.addView(logo, new LinearLayout.LayoutParams(dp(a, 52), dp(a, 52)));
        LinearLayout tb = new LinearLayout(a);
        tb.setOrientation(LinearLayout.VERTICAL);
        tb.setPadding(dp(a, 12), 0, 0, 0);
        TextView t1 = new TextView(a);
        t1.setText("MT Browser");
        t1.setTextSize(18);
        t1.setTextColor(l ? 0xFF14213D : Color.WHITE);
        t1.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView t2 = new TextView(a);
        t2.setText("Fast  •  Safe  •  Smart");
        t2.setTextSize(13);
        t2.setTextColor(sub);
        tb.addView(t1);
        tb.addView(t2);
        h.addView(tb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView pill = new ImageView(a); pill.setImageResource(R.drawable.ic_ghost_s); pill.setColorFilter(0xFFFFFFFF); pill.setPadding(dp(a, 15), dp(a, 15), dp(a, 15), dp(a, 15)); pill.setBackground(grad(0xFF8E6CFF, 0xFF5B3FD0, true, 0)); pill.setOnClickListener(v -> { d.dismiss(); a.newTab(true); }); press(pill); h.addView(pill, new LinearLayout.LayoutParams(dp(a, 52), dp(a, 52)));
        root.addView(h);

        LinearLayout r1 = row(a);
        r1.addView(circleItem(a, d, R.drawable.ic_m_bookmark, "Bookmarks", 0xFF21DDB8, 0xFF0E9F86, tx, () -> a.openLib("bookmarks")), w());
        r1.addView(circleItem(a, d, R.drawable.ic_m_history, "History", 0xFFFFCB45, 0xFFFF9500, tx, () -> a.openLib("history")), w());
        r1.addView(circleItem(a, d, R.drawable.ic_m_download, "Downloads", 0xFF63A8FF, 0xFF3563F5, tx, () -> a.openLib("downloads")), w());
        r1.addView(circleItem(a, d, R.drawable.ic_m_tools, "Tools", 0xFFB085FF, 0xFF7545F0, tx, () -> ToolsSheet.show(a)), w());
        r1.addView(circleItem(a, d, R.drawable.ic_m_qr, "QR Scan", 0xFFFFB05A, 0xFFFF6F2C, tx, () -> a.scanQr()), w());
        root.addView(r1);

        View line = new View(a);
        line.setBackgroundColor(l ? 0xFFDDE2EC : 0x1AFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 1));
        lp.topMargin = dp(a, 18);
        root.addView(line, lp);

        boolean night = !l;
        LinearLayout r2 = row(a);
        r2.addView(tileItem(a, d, R.drawable.ic_m_star, "Bookmark", 0xFFFFC53D, fill, stroke, tx, () -> a.addBookmark()), w());
        r2.addView(tileItem(a, d, R.drawable.ic_m_moon, "Night", night ? 0xFFCDBDFF : 0xFF8C9DFF,
                night ? 0x336C4DE0 : fill, night ? 0xFF6C4DE0 : stroke, tx, () -> {
            Prefs.setTheme(a, a.isLight() ? 0 : 1);
            a.applyTheme();
        }), w());
        r2.addView(tileItem(a, d, R.drawable.ic_m_refresh, "Refresh", 0xFF3DD6C6, fill, stroke, tx, () -> a.cur.wv.reload()), w());
        r2.addView(tileItem(a, d, R.drawable.ic_m_add, "New tab", 0xFF63A8FF, fill, stroke, tx, () -> a.newTab(false)), w());
        r2.addView(tileItem(a, d, R.drawable.ic_m_share, "Share", 0xFFFF86B0, fill, stroke, tx, () -> a.share()), w());
        root.addView(r2);

        int bgc = l ? 0x0F000000 : 0x14FFFFFF;
        LinearLayout bb = new LinearLayout(a);
        bb.setPadding(0, dp(a, 10), 0, 0);
        bb.addView(roundBtn(a, R.drawable.ic_m_settings, tx, bgc, v -> { d.dismiss(); a.openSettings(); }), w());
        bb.addView(roundBtn(a, R.drawable.ic_m_down, tx, bgc, v -> d.dismiss()), w());
        bb.addView(roundBtn(a, R.drawable.ic_m_power, 0xFFFF6B6B, bgc, v -> { d.dismiss(); a.finishAffinity(); }), w());
        root.addView(bb);

        d.setContentView(root);
        ((View) root.getParent()).setBackgroundColor(Color.TRANSPARENT);
        d.show();
        d.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        for (int i = 0; i < anim.size(); i++) {
            View v = anim.get(i);
            v.setAlpha(0f);
            v.setScaleX(0.6f);
            v.setScaleY(0.6f);
            v.setTranslationY(dp(a, 14));
            v.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setStartDelay(80 + i * 35L).setDuration(340)
                .setInterpolator(new OvershootInterpolator(1.3f)).start();
        }
    }
}
