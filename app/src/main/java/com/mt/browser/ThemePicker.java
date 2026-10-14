package com.mt.browser;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ThemePicker {
    static final int[] COL = {0xFF3B82F6, 0xFF8E5CFF, 0xFF14B8A6, 0xFF22C55E, 0xFFF5B301, 0xFFFF8F3A, 0xFFEC4899, 0xFFEF4444};

    static int dp(Activity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    static TextView label(Activity a, String t) {
        TextView v = new TextView(a);
        v.setText(t.toUpperCase());
        v.setTextSize(12);
        v.setLetterSpacing(0.1f);
        v.setTextColor(UiTheme.sub(a));
        v.setPadding(dp(a, 4), dp(a, 14), 0, dp(a, 8));
        return v;
    }

    public static View build(final Activity a, final Runnable done) {
        final int acc = UiTheme.acc(a), tx = UiTheme.tx(a);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);

        box.addView(label(a, "Background"));
        LinearLayout r1 = new LinearLayout(a);
        String[] mn = {"Navy", "Black", "White"};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            boolean on = UiTheme.mode(a) == i;
            TextView t = new TextView(a);
            t.setText(mn[i]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(14);
            t.setTypeface(null, android.graphics.Typeface.BOLD);
            t.setTextColor(on ? Color.WHITE : tx);
            GradientDrawable g = new GradientDrawable();
            g.setColor(on ? acc : UiTheme.chip(a));
            g.setCornerRadius(dp(a, 20));
            t.setBackground(g);
            t.setOnClickListener(v -> { UiTheme.setMode(a, k); done.run(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(a, 44), 1f);
            p.setMargins(dp(a, 4), 0, dp(a, 4), 0);
            r1.addView(t, p);
        }
        box.addView(r1);

        box.addView(label(a, "Accent colour"));
        LinearLayout r2 = new LinearLayout(a);
        for (int i = 0; i < COL.length; i++) {
            final int c = COL[i];
            FrameLayout cell = new FrameLayout(a);
            View dot = new View(a);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(c);
            if (c == acc) g.setStroke(dp(a, 3), tx);
            dot.setBackground(g);
            cell.addView(dot, new FrameLayout.LayoutParams(dp(a, 32), dp(a, 32), Gravity.CENTER));
            cell.setOnClickListener(v -> { UiTheme.setAccent(a, c); done.run(); });
            r2.addView(cell, new LinearLayout.LayoutParams(0, dp(a, 40), 1f));
        }
        box.addView(r2);

        box.addView(label(a, "Custom colour"));
        float[] hsv = new float[3];
        Color.colorToHSV(acc, hsv);
        SeekBar sb = new SeekBar(a);
        sb.setMax(360);
        sb.setProgress((int) hsv[0]);
        int[] hues = new int[13];
        for (int i = 0; i < 13; i++) hues[i] = Color.HSVToColor(new float[]{i * 30f % 360f, 0.8f, 0.95f});
        GradientDrawable spec = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, hues);
        spec.setCornerRadius(dp(a, 8));
        sb.setBackground(spec);
        sb.setProgressDrawable(new ColorDrawable(0));
        sb.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        sb.setPadding(dp(a, 14), 0, dp(a, 14), 0);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) {
                UiTheme.setAccent(a, Color.HSVToColor(new float[]{s.getProgress() % 360, 0.78f, 0.96f}));
                done.run();
            }
        });
        box.addView(sb, new LinearLayout.LayoutParams(-1, dp(a, 22)));
        return box;
    }

    public static void dialog(final Activity a) {
        final BottomSheetDialog d = new BottomSheetDialog(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a, 20), dp(a, 20), dp(a, 20), dp(a, 28));
        GradientDrawable bgd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{UiTheme.bg1(a), UiTheme.bg2(a)});
        float r = dp(a, 28);
        bgd.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        root.setBackground(bgd);
        TextView t = new TextView(a);
        t.setText("Theme");
        t.setTextSize(20);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(UiTheme.tx(a));
        root.addView(t);
        root.addView(build(a, () -> { d.dismiss(); a.recreate(); }));
        d.setContentView(root);
        ((View) root.getParent()).setBackgroundColor(Color.TRANSPARENT);
        d.show();
    }
}
