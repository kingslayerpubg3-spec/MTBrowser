package com.mt.browser;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MenuSheet {
    static int dp(MainActivity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }
    static LinearLayout.LayoutParams w() { return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); }

    static LinearLayout row(MainActivity a) {
        LinearLayout r = new LinearLayout(a);
        r.setPadding(0, dp(a, 10), 0, 0);
        return r;
    }

    static View item(final MainActivity a, final BottomSheetDialog d, int res, String label,
                     int color, int tx, final Runnable run) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(0, dp(a, 6), 0, dp(a, 6));
        ImageView ic = new ImageView(a);
        ic.setImageResource(res);
        int p = dp(a, color != 0 ? 15 : 12);
        ic.setPadding(p, p, p, p);
        if (color != 0) {
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(color);
            ic.setBackground(g);
            ic.setColorFilter(Color.WHITE);
        } else {
            ic.setColorFilter(tx);
        }
        c.addView(ic, new LinearLayout.LayoutParams(dp(a, 54), dp(a, 54)));
        TextView t = new TextView(a);
        t.setText(label);
        t.setTextSize(12);
        t.setTextColor(tx);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(a, 6), 0, 0);
        t.setMaxLines(2);
        c.addView(t);
        c.setOnClickListener(v -> { d.dismiss(); run.run(); });
        return c;
    }

    static ImageView bottomBtn(MainActivity a, int res, int tx, View.OnClickListener cl) {
        ImageView v = new ImageView(a);
        v.setImageResource(res);
        v.setColorFilter(tx);
        v.setPadding(dp(a, 10), dp(a, 12), dp(a, 10), dp(a, 8));
        v.setOnClickListener(cl);
        return v;
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
        final BottomSheetDialog d = new BottomSheetDialog(a);
        boolean l = a.isLight();
        int bg = l ? 0xFFF5F6F8 : 0xFF0F1A33;
        int tx = l ? 0xFF222222 : 0xFFFFFFFF;
        int sub = l ? 0xFF9AA0AA : 0xFF8FA0C0;

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a, 16), dp(a, 18), dp(a, 16), dp(a, 14));
        GradientDrawable rb = new GradientDrawable();
        rb.setColor(bg);
        float cr = dp(a, 24);
        rb.setCornerRadii(new float[]{cr, cr, cr, cr, 0, 0, 0, 0});
        root.setBackground(rb);

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
        t1.setTextColor(tx);
        t1.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView t2 = new TextView(a);
        t2.setText("Fast  •  Safe  •  Smart");
        t2.setTextSize(13);
        t2.setTextColor(sub);
        tb.addView(t1);
        tb.addView(t2);
        h.addView(tb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView pill = new TextView(a);
        pill.setText("Incognito");
        pill.setTextSize(13);
        pill.setTextColor(l ? 0xFF5B3FD0 : 0xFFC9B3FF);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ghost_s, 0, 0, 0);
        pill.setCompoundDrawablePadding(dp(a, 8));
        pill.setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 10));
        GradientDrawable pb = new GradientDrawable();
        pb.setColor(l ? 0xFFEDEBF8 : 0xFF2A1646);
        pb.setCornerRadius(dp(a, 22));
        pill.setBackground(pb);
        pill.setOnClickListener(v -> { d.dismiss(); a.newTab(true); });
        h.addView(pill);
        root.addView(h);

        LinearLayout r1 = row(a);
        r1.addView(item(a, d, R.drawable.ic_m_bookmark, "Bookmarks", 0xFF14C9A8, tx, () -> a.openLib("bookmarks")), w());
        r1.addView(item(a, d, R.drawable.ic_m_history, "History", 0xFFFFB300, tx, () -> a.openLib("history")), w());
        r1.addView(item(a, d, R.drawable.ic_m_download, "Downloads", 0xFF4C8DFF, tx, () -> a.openLib("downloads")), w());
        r1.addView(item(a, d, R.drawable.ic_m_tools, "Tools", 0xFF8E5CFF, tx, () -> tools(a)), w());
        r1.addView(item(a, d, R.drawable.ic_m_qr, "QR Scan", 0xFFFF8F3A, tx, () -> a.scanQr()), w());
        root.addView(r1);

        LinearLayout r2 = row(a);
        r2.addView(item(a, d, R.drawable.ic_m_star, "Add bookmark", 0, tx, () -> a.addBookmark()), w());
        r2.addView(item(a, d, R.drawable.ic_m_moon, "Night", 0, tx, () -> {
            Prefs.setTheme(a, a.isLight() ? 0 : 1);
            a.applyTheme();
        }), w());
        r2.addView(item(a, d, R.drawable.ic_m_refresh, "Refresh", 0, tx, () -> a.cur.wv.reload()), w());
        r2.addView(item(a, d, R.drawable.ic_m_add, "New tab", 0, tx, () -> a.newTab(false)), w());
        r2.addView(item(a, d, R.drawable.ic_m_share, "Share", 0, tx, () -> a.share()), w());
        root.addView(r2);

        View line = new View(a);
        line.setBackgroundColor(l ? 0xFFDDDFE3 : 0x33FFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 1));
        lp.topMargin = dp(a, 14);
        root.addView(line, lp);

        LinearLayout bb = new LinearLayout(a);
        bb.addView(bottomBtn(a, R.drawable.ic_m_settings, tx, v -> { d.dismiss(); a.openSettings(); }), w());
        bb.addView(bottomBtn(a, R.drawable.ic_m_down, tx, v -> d.dismiss()), w());
        bb.addView(bottomBtn(a, R.drawable.ic_m_power, tx, v -> { d.dismiss(); a.finishAffinity(); }), w());
        root.addView(bb);

        d.setContentView(root);
        ((View) root.getParent()).setBackgroundColor(Color.TRANSPARENT);
        d.show();
        d.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
