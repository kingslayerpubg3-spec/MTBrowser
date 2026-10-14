package com.mt.browser;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.view.View;

public class UiTheme {
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("mt", 0); }
    public static int mode(Context c) { return sp(c).getInt("ui_mode", 0); }
    public static int ver(Context c) { return sp(c).getInt("ui_ver", 0); }
    public static void setMode(Context c, int m) { sp(c).edit().putInt("ui_mode", m).putInt("ui_ver", ver(c) + 1).apply(); }
    public static int acc(Context c) { return sp(c).getInt("ui_acc", 0xFF3B82F6); }
    public static void setAccent(Context c, int a) { sp(c).edit().putInt("ui_acc", a).putInt("ui_ver", ver(c) + 1).apply(); }
    public static boolean light(Context c) { return mode(c) == 2; }
    static int pick(Context c, int a, int b, int w) { int m = mode(c); return m == 0 ? a : m == 1 ? b : w; }
    public static int bg1(Context c) { return pick(c, 0xFF0F1A4A, 0xFF000000, 0xFFF4F6FB); }
    public static int bg2(Context c) { return pick(c, 0xFF150F36, 0xFF000000, 0xFFE9EEF9); }
    public static int card(Context c) { return pick(c, 0xFF151D3E, 0xFF121212, 0xFFFFFFFF); }
    public static int stroke(Context c) { return pick(c, 0xFF232F55, 0xFF2A2A2A, 0xFFDCE2EF); }
    public static int chip(Context c) { return pick(c, 0xFF1B2547, 0xFF1C1C1C, 0xFFE6EBF5); }
    public static int tx(Context c) { return pick(c, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF14213D); }
    public static int sub(Context c) { return pick(c, 0xFF9AA9CC, 0xFF9A9A9A, 0xFF5A6785); }
    public static int acc2(Context c) {
        int a = acc(c);
        return Color.rgb((int) (Color.red(a) * 0.78f), (int) (Color.green(a) * 0.78f), (int) (Color.blue(a) * 0.78f));
    }
    public static void apply(Activity a) {
        a.getWindow().setStatusBarColor(bg1(a));
        a.getWindow().setNavigationBarColor(bg2(a));
        int f = 0;
        if (light(a)) {
            f = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        a.getWindow().getDecorView().setSystemUiVisibility(f);
    }
}
