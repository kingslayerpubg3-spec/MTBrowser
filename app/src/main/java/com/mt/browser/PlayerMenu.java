package com.mt.browser;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlayerMenu {
    static int dp(PlayerActivity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    static void show(final PlayerActivity a) {
        final Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(a, 14), dp(a, 16), dp(a, 14), dp(a, 12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF2101830);
        bg.setCornerRadius(dp(a, 24));
        bg.setStroke(dp(a, 1), 0x33FFFFFF);
        box.setBackground(bg);
        final int[] ic = {R.drawable.ic_pl_cc, R.drawable.ic_pl_audio, R.drawable.ic_pl_aspect,
                R.drawable.ic_pl_loop, R.drawable.ic_pl_open, R.drawable.ic_pl_share};
        final String[] lb = {"Subtitle", "Audio track", "Aspect: " + PlayerActivity.RN[a.resize],
                "Loop: " + (a.loop ? "On" : "Off"), "Other player", "Share link"};
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(a);
            for (int c = 0; c < 3; c++) {
                final int k = r * 3 + c;
                LinearLayout cell = new LinearLayout(a);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(0, dp(a, 8), 0, dp(a, 8));
                ImageView iv = new ImageView(a);
                iv.setImageResource(ic[k]);
                iv.setColorFilter(k == 3 && a.loop ? UiTheme.acc(a) : Color.WHITE);
                int p = dp(a, 15);
                iv.setPadding(p, p, p, p);
                GradientDrawable tg = new GradientDrawable();
                tg.setColor(0x22FFFFFF);
                tg.setCornerRadius(dp(a, 18));
                iv.setBackground(tg);
                cell.addView(iv, new LinearLayout.LayoutParams(dp(a, 58), dp(a, 58)));
                TextView t = new TextView(a);
                t.setText(lb[k]);
                t.setTextColor(Color.WHITE);
                t.setTextSize(12);
                t.setGravity(Gravity.CENTER);
                t.setPadding(0, dp(a, 6), 0, 0);
                cell.addView(t);
                cell.setOnClickListener(x -> { d.dismiss(); a.menuAct(k); });
                MenuSheet.press(cell);
                row.addView(cell, new LinearLayout.LayoutParams(dp(a, 94), -2));
            }
            box.addView(row);
        }
        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(-2, -2);
            w.setWindowAnimations(android.R.style.Animation_Dialog);
        }
        d.show();
    }
}
