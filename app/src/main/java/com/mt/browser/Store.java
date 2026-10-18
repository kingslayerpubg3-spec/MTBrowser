package com.mt.browser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public class Store {
    static final java.util.concurrent.ExecutorService HEX = java.util.concurrent.Executors.newSingleThreadExecutor();
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("mt", 0); }

    public static JSONArray get(Context c, String key) {
        try { return new JSONArray(sp(c).getString(key, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    static void put(Context c, String key, JSONArray a) {
        sp(c).edit().putString(key, a.toString()).apply();
    }

    static void addTo(Context c, String key, String title, String url, int max) {
        JSONArray a = get(c, key);
        JSONArray n = new JSONArray();
        try {
            JSONObject o = new JSONObject();
            o.put("title", (title == null || title.isEmpty()) ? url : title);
            o.put("url", url);
            n.put(o);
            for (int i = 0; i < a.length() && n.length() < max; i++) {
                if (!a.getJSONObject(i).getString("url").equals(url)) n.put(a.get(i));
            }
        } catch (Exception e) { }
        put(c, key, n);
    }

    public static void addBookmark(Context c, String title, String url) { addTo(c, "bm", title, url, 500); }

    public static void addHistory(Context c, String title, String url) {
        if (url == null || url.startsWith("file:") || url.startsWith("about:")) return;
        final Context cc = c.getApplicationContext(); final String ft = title, fu = url; HEX.execute(new Runnable() { public void run() { addTo(cc, "hist", ft, fu, 200); } });
    }

    public static void remove(Context c, String key, int idx) {
        JSONArray a = get(c, key);
        JSONArray n = new JSONArray();
        try { for (int i = 0; i < a.length(); i++) if (i != idx) n.put(a.get(i)); }
        catch (Exception e) { }
        put(c, key, n);
    }

    public static void clear(Context c, String key) { put(c, key, new JSONArray()); }
}
