package com.mt.browser;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    public static final String[] NAMES = {"Google", "Bing", "DuckDuckGo"};
    static final String[] URLS = {
        "https://www.google.com/search?q=",
        "https://www.bing.com/search?q=",
        "https://duckduckgo.com/?q="
    };
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("mt", 0); }
    public static boolean adBlock(Context c) { return sp(c).getBoolean("adblock", true); }
    public static void setAdBlock(Context c, boolean v) { sp(c).edit().putBoolean("adblock", v).apply(); }
    public static int engine(Context c) { return sp(c).getInt("engine", 0); }
    public static void setEngine(Context c, int i) { sp(c).edit().putInt("engine", i).apply(); }
    public static String engineName(Context c) { return NAMES[engine(c)]; }
    public static String searchUrl(Context c) { return URLS[engine(c)]; }
    public static int theme(Context c) { return sp(c).getInt("theme", 0); }
    public static void setTheme(Context c, int i) { sp(c).edit().putInt("theme", i).apply(); }
    public static String themeName(Context c) { return new String[]{"Dark", "Light", "Auto (system)"}[theme(c)]; }
    public static boolean wifiOnly(Context c) { return sp(c).getBoolean("wifionly", false); }
    public static void setWifiOnly(Context c, boolean v) { sp(c).edit().putBoolean("wifionly", v).apply(); }
    public static boolean clipCatch(Context c) { return sp(c).getBoolean("clipcatch", true); }
    public static void setClipCatch(Context c, boolean v) { sp(c).edit().putBoolean("clipcatch", v).apply(); }
    public static boolean builtinPlayer(Context c) { return sp(c).getBoolean("bplayer", true); }
    public static void setBuiltinPlayer(Context c, boolean v) { sp(c).edit().putBoolean("bplayer", v).apply(); }
}
