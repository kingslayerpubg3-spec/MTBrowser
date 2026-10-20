package com.mt.browser;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ToolsSheet {
    static int dp(MainActivity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    static void show(final MainActivity a) {
        final BottomSheetDialog d = new BottomSheetDialog(a);
        int tx = UiTheme.tx(a), sub = UiTheme.sub(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a, 18), dp(a, 12), dp(a, 18), dp(a, 16));
        GradientDrawable rb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{UiTheme.bg1(a), UiTheme.bg2(a)});
        float cr = dp(a, 28);
        rb.setCornerRadii(new float[]{cr, cr, cr, cr, 0, 0, 0, 0});
        root.setBackground(rb);

        View handle = new View(a);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(UiTheme.stroke(a));
        hg.setCornerRadius(dp(a, 3));
        handle.setBackground(hg);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(a, 40), dp(a, 4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = dp(a, 14);
        root.addView(handle, hp);

        TextView title = new TextView(a);
        title.setText("Tools");
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(tx);
        root.addView(title);
        TextView st = new TextView(a);
        st.setText("Page par kaam ki cheezein");
        st.setTextSize(13);
        st.setTextColor(sub);
        st.setPadding(0, dp(a, 2), 0, dp(a, 12));
        root.addView(st);

        String[] n = {"Translate page", "Find in page", "Reader mode", "Save as PDF", "Page screenshot", "Copy page text",
                      "Copy link", "Desktop site", "Pop-up video", "Smart Download"};
        String[] s = {"Kisi bhi bhasha me badlo", "Page me word dhundo", "Sirf article padho, saaf layout me", "Page ka PDF banao",
                      "Dikhne wala hissa photo me save", "Poora text copy karo", "Page ka link copy karo", "Computer wala version kholo",
                      "Video chhoti window me chalao", "Page ka video ya file detect karo"};
        int[] ic = {R.drawable.ic_m_translate, R.drawable.ic_search, R.drawable.ic_m_reader, R.drawable.ic_m_file, R.drawable.ic_pl_shot,
                    R.drawable.ic_m_copy, R.drawable.ic_m_link, R.drawable.ic_m_desktop, R.drawable.ic_m_pip, R.drawable.ic_m_download};
        int[] c1 = {0xFF63A8FF, 0xFFFFCB45, 0xFF21DDB8, 0xFFFF7A7A, 0xFFB085FF, 0xFF3DD6C6, 0xFF8E9DFF, 0xFF63A8FF, 0xFFFF86B0, 0xFFFFB05A};
        int[] c2 = {0xFF3563F5, 0xFFFF9500, 0xFF0E9F86, 0xFFE03A3A, 0xFF7545F0, 0xFF0E8F94, 0xFF5B6BFF, 0xFF3563F5, 0xFFE0457B, 0xFFFF6F2C};

        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < n.length; i++) {
            final int k = i;
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(a, 12), dp(a, 11), dp(a, 12), dp(a, 11));
            GradientDrawable g = new GradientDrawable();
            g.setColor(UiTheme.card(a));
            g.setCornerRadius(dp(a, 18));
            g.setStroke(dp(a, 1), UiTheme.stroke(a));
            row.setBackground(g);
            ImageView iv = new ImageView(a);
            iv.setImageResource(ic[i]);
            iv.setColorFilter(Color.WHITE);
            iv.setPadding(dp(a, 11), dp(a, 11), dp(a, 11), dp(a, 11));
            GradientDrawable ig = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1[i], c2[i]});
            ig.setCornerRadius(dp(a, 14));
            iv.setBackground(ig);
            row.addView(iv, new LinearLayout.LayoutParams(dp(a, 46), dp(a, 46)));
            LinearLayout tb = new LinearLayout(a);
            tb.setOrientation(LinearLayout.VERTICAL);
            TextView t1 = new TextView(a);
            t1.setText(n[i]);
            t1.setTextSize(15);
            t1.setTypeface(null, android.graphics.Typeface.BOLD);
            t1.setTextColor(tx);
            TextView t2 = new TextView(a);
            t2.setText(s[i]);
            t2.setTextSize(12);
            t2.setTextColor(sub);
            tb.addView(t1);
            tb.addView(t2);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
            tp.setMargins(dp(a, 14), 0, 0, 0);
            row.addView(tb, tp);
            row.setOnClickListener(v -> {
                d.dismiss();
                switch (k) {
                    case 0: a.translate(); break;
                    case 1: a.findInPage(); break;
                    case 2: a.readerMode(); break;
                    case 3: a.savePdf(); break;
                    case 4: a.pageShot(); break;
                    case 5: a.copyText(); break;
                    case 6: a.copyLink(); break;
                    case 7: a.toggleDesktop(); break;
                    case 8: a.enterPip(); break;
                    case 9: a.smartDownload(); break;
                }
            });
            row.setAlpha(0f);
            row.setTranslationY(dp(a, 12));
            row.animate().alpha(1f).translationY(0f).setStartDelay(60 + i * 35L).setDuration(260).start();
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.bottomMargin = dp(a, 8);
            list.addView(row, rp);
        }
        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(list);
        root.addView(sv);
        d.setContentView(root);
        ((View) root.getParent()).setBackgroundColor(Color.TRANSPARENT);
        d.show();
        d.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
