package com.mt.browser;

import java.util.Locale;

public class Sniffer {
    static final String[] EXT = {".mp4", ".webm", ".mkv", ".m4v", ".mov", ".3gp", ".mp3", ".m4a", ".aac"};

    static void see(MainActivity a, MainActivity.Tab t, String u) {
        if (u == null || t == null || u.length() < 8) return;
        int q = u.indexOf('?');
        String p = q > 0 ? u.substring(0, q) : u;
        int n = p.length();
        if (n < 5) return;
        String tail = p.substring(Math.max(0, n - 6)).toLowerCase(Locale.US);
        boolean hls = tail.endsWith(".m3u8") || tail.endsWith(".mpd");
        boolean media = false;
        if (!hls) {
            for (String x : EXT) if (tail.endsWith(x)) { media = true; break; }
        }
        if (!hls && !media) return;
        try {
            if (AdBlock.isAd(android.net.Uri.parse(u).getHost())) return;
        } catch (Exception e) { }
        if (media) {
            boolean added;
            synchronized (t.sniffed) {
                if (t.sniffed.size() >= 15) return;
                added = t.sniffed.add(u);
            }
            if (added) {
                a.runOnUiThread(a::updateDlFab);
                ProbeCache.async(u);
            }
        } else {
            t.hls = true;
            if (tail.endsWith(".m3u8")) {
                synchronized (t.hlsList) {
                    if (t.hlsList.size() < 10 && !t.hlsList.contains(u)) t.hlsList.add(u);
                }
            }
        }
    }

    static void reset(MainActivity a, MainActivity.Tab t) {
        t.sniffed.clear();
        a.inlineStop(t, false);
        t.vurl = "";
        t.vtitle = "";
        t.vplaying = false;
        t.hls = false;
        t.hlsList.clear();
        t.scraped.clear();
        a.updateDlFab();
    }
}
