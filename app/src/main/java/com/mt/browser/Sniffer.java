package com.mt.browser;

import android.net.Uri;

public class Sniffer {
    static void see(MainActivity a, MainActivity.Tab t, String u) {
        if (u == null || t == null) return;
        try {
            String host = Uri.parse(u).getHost();
            if (AdBlock.isAd(host)) return;
        } catch (Exception e) { }
        String l = u.toLowerCase();
        int q = l.indexOf('?');
        String p = q > 0 ? l.substring(0, q) : l;
        if (p.matches(".*\\.(mp4|webm|mkv|m4v|mov|3gp|mp3|m4a|aac)$")) {
            boolean added;
            synchronized (t.sniffed) {
                if (t.sniffed.size() >= 15) return;
                added = t.sniffed.add(u);
            }
            if (added) a.runOnUiThread(a::updateDlFab);
        } else if (p.endsWith(".m3u8") || p.endsWith(".mpd")) {
            t.hls = true;
            if (p.endsWith(".m3u8")) { synchronized (t.hlsList) { if (t.hlsList.size() < 10 && !t.hlsList.contains(u)) t.hlsList.add(u); } }
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
