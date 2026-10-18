package com.mt.browser;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProbeCache {
    static final ConcurrentHashMap<String, long[]> map = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Long> when = new ConcurrentHashMap<>();
    static final ExecutorService ex = Executors.newFixedThreadPool(4);
    public static volatile String ua = "";
    public static volatile String ref = "";

    static long[] get(String u) {
        Long t = when.get(u);
        long[] v = map.get(u);
        if (t == null || v == null) return null;
        long age = System.currentTimeMillis() - t;
        if (age > (v[0] > 0 ? 300000L : 60000L)) return null;
        return v;
    }

    static void put(String u, long[] v) {
        if (u == null || v == null) return;
        if (map.size() > 200) { map.clear(); when.clear(); }
        map.put(u, v);
        when.put(u, System.currentTimeMillis());
    }

    public static void async(final String u) {
        if (u == null || get(u) != null) return;
        ex.execute(new Runnable() {
            public void run() {
                DownloadEngine.Item it = new DownloadEngine.Item();
                it.url = u;
                it.ua = ua;
                it.ref = ref;
                put(u, DownloadEngine.probeRaw(it));
            }
        });
    }
}
