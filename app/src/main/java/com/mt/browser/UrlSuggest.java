package com.mt.browser;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;

public class UrlSuggest {
    static PopupWindow pw;
    static LinearLayout box;

    static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density); }

    public static void attach(final MainActivity a, final EditText bar) {
        bar.setSelectAllOnFocus(true);
        bar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int x, int y, int z) { }
            public void onTextChanged(CharSequence s, int x, int y, int z) {
                if (!bar.hasFocus()) { dismiss(); return; }
                update(a, bar, s.toString().trim());
            }
            public void afterTextChanged(Editable e) { }
        });
        bar.setOnFocusChangeListener((v, f) -> { if (!f) dismiss(); });
    }

    static void dismiss() {
        try { if (pw != null && pw.isShowing()) pw.dismiss(); } catch (Exception e) { }
    }

    static void collect(Context c, String key, String q, ArrayList<String[]> rows, HashSet<String> seen, String type, int max) {
        JSONArray arr = Store.get(c, key);
        int n = 0;
        for (int i = 0; i < arr.length() && n < max; i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                String t = o.optString("title"), u = o.optString("url");
                if (u.isEmpty() || seen.contains(u)) continue;
                if (!t.toLowerCase().contains(q) && !u.toLowerCase().contains(q)) continue;
                seen.add(u);
                rows.add(new String[]{t.isEmpty() ? u : t, u, type});
                n++;
            } catch (Exception e) { }
        }
    }

    static void update(final MainActivity a, final EditText bar, final String q) {
        if (q.isEmpty() || bar.getWidth() <= 0) { dismiss(); return; }
        String ql = q.toLowerCase();
        boolean inc = a.cur != null && a.cur.incog;
        ArrayList<String[]> rows = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        boolean looksUrl = q.indexOf(' ') < 0 && (q.contains(".") || q.startsWith("http"));
        rows.add(new String[]{looksUrl ? "Kholo: " + q : "Search: " + q, q, looksUrl ? "o" : "s"});
        collect(a, "bm", ql, rows, seen, "b", 3);
        if (!inc) collect(a, "hist", ql, rows, seen, "h", 4);
        if (pw == null || box == null || box.getContext() != a) {
            dismiss();
            box = new LinearLayout(a);
            box.setOrientation(LinearLayout.VERTICAL);
            pw = new PopupWindow(box, bar.getWidth(), -2, false);
            pw.setOutsideTouchable(true);
            pw.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
            pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            pw.setElevation(dp(a, 10));
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(UiTheme.card(a));
        bg.setCornerRadius(dp(a, 20));
        bg.setStroke(dp(a, 1), UiTheme.stroke(a));
        box.setBackground(bg);
        box.setPadding(0, dp(a, 6), 0, dp(a, 6));
        box.removeAllViews();
        for (String[] r : rows) box.addView(row(a, bar, r[0], r[1], r[2]), new LinearLayout.LayoutParams(-1, -2));
        try {
            if (!pw.isShowing()) pw.showAsDropDown(bar, 0, dp(a, 4));
            else pw.update(bar, 0, dp(a, 4), bar.getWidth(), -2);
        } catch (Exception e) { }
    }

    static View row(final MainActivity a, final EditText bar, String title, final String value, String type) {
        LinearLayout r = new LinearLayout(a);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(a, 14), dp(a, 9), dp(a, 14), dp(a, 9));
        int res = type.equals("b") ? R.drawable.ic_m_bookmark : type.equals("h") ? R.drawable.ic_m_history
                : type.equals("o") ? R.drawable.ic_m_globe : R.drawable.ic_search;
        int col = type.equals("b") ? 0xFFFFB300 : type.equals("h") ? 0xFF8E5CFF : UiTheme.acc(a);
        ImageView ic = new ImageView(a);
        ic.setImageResource(res);
        ic.setColorFilter(Color.WHITE);
        int p = dp(a, 7);
        ic.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(col);
        ic.setBackground(g);
        r.addView(ic, new LinearLayout.LayoutParams(dp(a, 32), dp(a, 32)));
        LinearLayout mid = new LinearLayout(a);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView t1 = new TextView(a);
        t1.setText(title);
        t1.setTextSize(14);
        t1.setTextColor(UiTheme.tx(a));
        t1.setSingleLine(true);
        t1.setEllipsize(TextUtils.TruncateAt.END);
        mid.addView(t1);
        if (type.equals("b") || type.equals("h")) {
            TextView t2 = new TextView(a);
            t2.setText(value.replaceFirst("^https?://(www\\.)?", ""));
            t2.setTextSize(12);
            t2.setTextColor(UiTheme.sub(a));
            t2.setSingleLine(true);
            t2.setEllipsize(TextUtils.TruncateAt.END);
            mid.addView(t2);
        }
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -2, 1f);
        mp.setMargins(dp(a, 12), 0, 0, 0);
        r.addView(mid, mp);
        r.setOnClickListener(v -> {
            dismiss();
            try {
                ((InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(bar.getWindowToken(), 0);
            } catch (Exception e) { }
            bar.clearFocus();
            a.load(value);
        });
        return r;
    }
}
