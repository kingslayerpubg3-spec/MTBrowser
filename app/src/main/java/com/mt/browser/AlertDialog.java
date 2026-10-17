package com.mt.browser;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class AlertDialog {
    final Dialog dlg;

    AlertDialog(Dialog d) { dlg = d; }

    public void dismiss() { try { dlg.dismiss(); } catch (Exception e) { } }

    public boolean isShowing() { return dlg != null && dlg.isShowing(); }

    static final int[][] PAL = {
        {0xFF63A8FF, 0xFF3563F5}, {0xFFB085FF, 0xFF7545F0}, {0xFF21DDB8, 0xFF0E9F86},
        {0xFFFFCB45, 0xFFFF9500}, {0xFFFFB05A, 0xFFFF6F2C}, {0xFFFF86B0, 0xFFE0457B}};
    static final int[] RED = {0xFFFF7A7A, 0xFFE03A3A};

    static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density); }

    static boolean has(String s, String... k) {
        for (String x : k) if (s.contains(x)) return true;
        return false;
    }

    static boolean isDanger(String raw) {
        String s = raw == null ? "" : raw.toLowerCase();
        return has(s, "delete", "cancel", "block", "clear", "remove", "hata");
    }

    static int iconFor(String raw) {
        String s = raw == null ? "" : raw.toLowerCase().trim();
        if (has(s, "subtitle")) return R.drawable.ic_m_cc;
        if (has(s, "audio")) return R.drawable.ic_m_audio;
        if (has(s, "aspect")) return R.drawable.ic_m_aspect;
        if (has(s, "loop")) return R.drawable.ic_m_loop;
        if (has(s, "dusre player", "other app", "mx player", "open with")) return R.drawable.ic_m_open;
        if (has(s, "share")) return R.drawable.ic_m_share;
        if (s.matches("^[0-9.]+x$")) return R.drawable.ic_m_speed;
        if (has(s, "incognito")) return R.drawable.ic_ghost_w;
        if (has(s, "rename")) return R.drawable.ic_m_edit;
        if (has(s, "folder")) return R.drawable.ic_m_folder;
        if (has(s, "re-download", "retry", "refresh", "reload")) return R.drawable.ic_m_refresh;
        if (has(s, "details", "about")) return R.drawable.ic_m_info;
        if (has(s, "delete", "hata", "clear", "remove")) return R.drawable.ic_m_delete;
        if (has(s, "cancel", "block", "band")) return R.drawable.ic_m_close;
        if (has(s, "pause")) return R.drawable.ic_m_pause;
        if (has(s, "resume")) return R.drawable.ic_m_play;
        if (has(s, "check & fix", "seekable", "tools")) return R.drawable.ic_m_tools;
        if (has(s, "translate", "hindi", "english", "urdu", "bengali", "tamil", "telugu", "marathi", "gujarati", "punjabi")) return R.drawable.ic_m_translate;
        if (has(s, "copy")) return R.drawable.ic_m_copy;
        if (has(s, "download")) return R.drawable.ic_m_download;
        if (has(s, "image")) return R.drawable.ic_m_image;
        if (has(s, "naye tab", "new tab")) return R.drawable.ic_m_add;
        if (has(s, "google", "bing", "duckduckgo")) return R.drawable.ic_m_globe;
        if (has(s, "text", "small", "normal", "large")) return R.drawable.ic_m_text;
        if (has(s, "dark", "light", "navy", "black", "white")) return R.drawable.ic_m_moon;
        if (s.matches("^(auto|\\d{3,4}p)$")) return R.drawable.ic_m_play;
        if (s.matches("^\\d+$")) return R.drawable.ic_m_download;
        if (s.matches(".*\\.(mp4|mkv|webm|3gp|avi|mov|m4v|ts)$")) return R.drawable.ic_m_play;
        if (s.matches(".*\\.(mp3|m4a|aac|wav|ogg|flac)$")) return R.drawable.ic_m_music;
        if (s.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) return R.drawable.ic_m_image;
        return R.drawable.ic_m_file;
    }

    static int titleIcon(String raw) {
        String s = raw == null ? "" : raw.toLowerCase();
        if (has(s, "delete", "clear", "hata")) return R.drawable.ic_m_delete;
        if (has(s, "permission", "location", "safe", "redirect", "dusra app", "naya tab", "popup")) return R.drawable.ic_m_shield;
        if (has(s, "rename", "file name")) return R.drawable.ic_m_edit;
        if (has(s, "find")) return R.drawable.ic_search;
        if (has(s, "link")) return R.drawable.ic_m_link;
        if (has(s, "download")) return R.drawable.ic_m_download;
        if (has(s, "video", "player")) return R.drawable.ic_m_play;
        if (has(s, "qr")) return R.drawable.ic_m_qr;
        return R.drawable.ic_m_info;
    }

    public static class Builder {
        final Context c;
        CharSequence title, msg, pos, neg, neu;
        CharSequence[] items;
        DialogInterface.OnClickListener itemL, posL, negL, neuL;
        DialogInterface.OnCancelListener cancelL;
        View view;
        final java.util.ArrayList<View> anim = new java.util.ArrayList<>();

        public Builder(Context c) { this.c = c; }
        public Builder setTitle(CharSequence t) { title = t; return this; }
        public Builder setMessage(CharSequence m) { msg = m; return this; }
        public Builder setView(View v) { view = v; return this; }
        public Builder setCancelable(boolean b) { return this; }
        public Builder setItems(CharSequence[] it, DialogInterface.OnClickListener l) { items = it; itemL = l; return this; }
        public Builder setPositiveButton(CharSequence t, DialogInterface.OnClickListener l) { pos = t; posL = l; return this; }
        public Builder setNegativeButton(CharSequence t, DialogInterface.OnClickListener l) { neg = t; negL = l; return this; }
        public Builder setNeutralButton(CharSequence t, DialogInterface.OnClickListener l) { neu = t; neuL = l; return this; }
        public Builder setOnCancelListener(DialogInterface.OnCancelListener l) { cancelL = l; return this; }

        public AlertDialog create() { return new AlertDialog(items != null ? sheet() : card()); }

        public AlertDialog show() {
            AlertDialog a = create();
            try {
                a.dlg.show();
                if (a.dlg instanceof BottomSheetDialog) {
                    BottomSheetBehavior<android.widget.FrameLayout> b = ((BottomSheetDialog) a.dlg).getBehavior();
                    b.setState(BottomSheetBehavior.STATE_EXPANDED);
                    b.setSkipCollapsed(true);
                }
                for (int i = 0; i < anim.size(); i++)
                    anim.get(i).animate().alpha(1f).translationY(0f).setStartDelay(40 + i * 35L).setDuration(260).start();
            } catch (Exception e) { }
            return a;
        }

        TextView txt(String t, int sp, int col, boolean bold) {
            TextView v = new TextView(c);
            v.setText(t);
            v.setTextSize(sp);
            v.setTextColor(col);
            if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
            return v;
        }

        View btn(CharSequence t, int type, final Dialog d, final DialogInterface.OnClickListener l, final int which) {
            TextView b = new TextView(c);
            b.setText(t);
            b.setGravity(Gravity.CENTER);
            b.setTextSize(16);
            b.setTypeface(null, android.graphics.Typeface.BOLD);
            int acc = UiTheme.acc(c);
            GradientDrawable g;
            if (type == 0) {
                boolean red = isDanger(String.valueOf(t));
                g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        red ? new int[]{0xFFFF6B6B, 0xFFE03A3A} : new int[]{acc, UiTheme.acc2(c)});
                b.setTextColor(Color.WHITE);
            } else if (type == 1) {
                g = new GradientDrawable();
                g.setColor(Color.TRANSPARENT);
                g.setStroke(dp(c, 1), acc);
                b.setTextColor(acc);
            } else {
                g = new GradientDrawable();
                g.setColor(Color.TRANSPARENT);
                b.setTextColor(UiTheme.sub(c));
            }
            g.setCornerRadius(dp(c, 24));
            b.setBackground(g);
            b.setOnClickListener(v -> { d.dismiss(); if (l != null) l.onClick(d, which); });
            MenuSheet.press(b);
            return b;
        }

        void buttons(LinearLayout root, Dialog d, boolean needDefault) {
            if (pos == null && neg == null && neu == null && needDefault) pos = "OK";
            if (pos != null) addBtn(root, btn(pos, 0, d, posL, DialogInterface.BUTTON_POSITIVE));
            if (neu != null) addBtn(root, btn(neu, 1, d, neuL, DialogInterface.BUTTON_NEUTRAL));
            if (neg != null) addBtn(root, btn(neg, 2, d, negL, DialogInterface.BUTTON_NEGATIVE));
        }

        void addBtn(LinearLayout root, View b) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(c, 48));
            lp.topMargin = dp(c, 8);
            root.addView(b, lp);
        }

        Dialog sheet() {
            final BottomSheetDialog d = new BottomSheetDialog(c);
            int tx = UiTheme.tx(c), sub = UiTheme.sub(c);
            LinearLayout root = new LinearLayout(c);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 20));
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{UiTheme.bg1(c), UiTheme.bg2(c)});
            float r = dp(c, 28);
            bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            root.setBackground(bg);
            View handle = new View(c);
            GradientDrawable hg = new GradientDrawable();
            hg.setColor(UiTheme.stroke(c));
            hg.setCornerRadius(dp(c, 3));
            handle.setBackground(hg);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(c, 40), dp(c, 4));
            hp.gravity = Gravity.CENTER_HORIZONTAL;
            hp.bottomMargin = dp(c, 14);
            root.addView(handle, hp);
            if (title != null) {
                TextView t = txt(title.toString(), 20, tx, true);
                t.setMaxLines(2);
                t.setEllipsize(TextUtils.TruncateAt.END);
                t.setPadding(0, 0, 0, dp(c, 12));
                root.addView(t);
            }
            if (msg != null) {
                TextView m = txt(msg.toString(), 14, sub, false);
                m.setPadding(0, 0, 0, dp(c, 12));
                root.addView(m);
            }
            ScrollView sv = new ScrollView(c) {
                @Override protected void onMeasure(int w, int h) {
                    int max = (int) (getResources().getDisplayMetrics().heightPixels * 0.6f);
                    super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST));
                }
            };
            sv.setVerticalScrollBarEnabled(false);
            LinearLayout list = new LinearLayout(c);
            list.setOrientation(LinearLayout.VERTICAL);
            for (int i = 0; i < items.length; i++) {
                final int idx = i;
                String s = String.valueOf(items[i]);
                boolean danger = isDanger(s);
                LinearLayout row = new LinearLayout(c);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(c, 12), dp(c, 10), dp(c, 14), dp(c, 10));
                GradientDrawable g = new GradientDrawable();
                g.setColor(UiTheme.card(c));
                g.setCornerRadius(dp(c, 16));
                g.setStroke(dp(c, 1), UiTheme.stroke(c));
                row.setBackground(g);
                ImageView ic = new ImageView(c);
                ic.setImageResource(iconFor(s));
                ic.setColorFilter(Color.WHITE);
                int p = dp(c, 10);
                ic.setPadding(p, p, p, p);
                GradientDrawable ig = new GradientDrawable(GradientDrawable.Orientation.TL_BR, danger ? RED : PAL[i % PAL.length]);
                ig.setCornerRadius(dp(c, 12));
                ic.setBackground(ig);
                row.addView(ic, new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)));
                TextView t = txt(s, 16, danger ? 0xFFEF4444 : tx, false);
                t.setMaxLines(2);
                t.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
                tp.setMargins(dp(c, 14), 0, 0, 0);
                row.addView(t, tp);
                row.setOnClickListener(v -> { d.dismiss(); if (itemL != null) itemL.onClick(d, idx); });
                MenuSheet.press(row);
                row.setAlpha(0f);
                row.setTranslationY(dp(c, 12));
                anim.add(row);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                rp.bottomMargin = dp(c, 8);
                list.addView(row, rp);
            }
            sv.addView(list);
            root.addView(sv);
            buttons(root, d, false);
            d.setContentView(root);
            ((View) root.getParent()).setBackgroundColor(Color.TRANSPARENT);
            if (cancelL != null) d.setOnCancelListener(cancelL);
            return d;
        }

        Dialog card() {
            final Dialog d = new Dialog(c);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            int tx = UiTheme.tx(c), sub = UiTheme.sub(c);
            LinearLayout box = new LinearLayout(c);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(c, 22), dp(c, 22), dp(c, 22), dp(c, 14));
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{UiTheme.bg1(c), UiTheme.bg2(c)});
            bg.setCornerRadius(dp(c, 28));
            bg.setStroke(dp(c, 1), UiTheme.stroke(c));
            box.setBackground(bg);
            String tl = title == null ? "" : title.toString();
            ImageView ic = new ImageView(c);
            ic.setImageResource(titleIcon(tl));
            ic.setColorFilter(Color.WHITE);
            int p = dp(c, 12);
            ic.setPadding(p, p, p, p);
            GradientDrawable ig = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    isDanger(tl) ? RED : new int[]{UiTheme.acc(c), UiTheme.acc2(c)});
            ig.setShape(GradientDrawable.OVAL);
            ic.setBackground(ig);
            box.addView(ic, new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
            if (title != null) {
                TextView t = txt(tl, 20, tx, true);
                t.setPadding(0, dp(c, 12), 0, 0);
                box.addView(t);
            }
            if (msg != null) {
                ScrollView ms = new ScrollView(c) {
                    @Override protected void onMeasure(int w, int h) {
                        int max = (int) (getResources().getDisplayMetrics().heightPixels * 0.45f);
                        super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST));
                    }
                };
                ms.setVerticalScrollBarEnabled(false);
                TextView m = txt(msg.toString(), 15, sub, false);
                m.setPadding(0, dp(c, 8), 0, dp(c, 4));
                ms.addView(m);
                box.addView(ms);
            }
            if (view != null) {
                ViewGroup pv = (ViewGroup) view.getParent();
                if (pv != null) pv.removeView(view);
                if (view instanceof EditText) {
                    EditText e = (EditText) view;
                    e.setTextColor(tx);
                    e.setHintTextColor(sub);
                    GradientDrawable eg = new GradientDrawable();
                    eg.setColor(UiTheme.chip(c));
                    eg.setCornerRadius(dp(c, 16));
                    e.setBackground(eg);
                    e.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
                }
                LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
                vp.topMargin = dp(c, 14);
                box.addView(view, vp);
            }
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(c, 8));
            box.addView(new View(c), sp);
            buttons(box, d, true);
            d.setContentView(box);
            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                DisplayMetrics dm = c.getResources().getDisplayMetrics();
                int wd = (int) Math.min(dm.widthPixels * 0.88f, dp(c, 420));
                w.setLayout(wd, ViewGroup.LayoutParams.WRAP_CONTENT);
                w.setWindowAnimations(android.R.style.Animation_Dialog);
                if (view != null)
                    w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
            if (cancelL != null) d.setOnCancelListener(cancelL);
            return d;
        }
    }
}
