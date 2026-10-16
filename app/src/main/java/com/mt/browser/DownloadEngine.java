package com.mt.browser;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DownloadEngine {
    public static final int ACTIVE = 0, PAUSED = 1, DONE = 2, FAILED = 3;

    public static class Item {
        public long id;
        public String name = "", url = "", ua = "", uri = "", err = "";
        public String segs = "";
        public String ref = "";
        public volatile long total, done;
        public volatile int status;
        public volatile double speed;
        public volatile boolean stop, running;
    }

    public static final ArrayList<Item> items = new ArrayList<>();
    static Context app;
    static boolean loaded;

    public static synchronized void init(Context c) {
        if (loaded) return;
        app = c.getApplicationContext();
        loaded = true;
        try {
            JSONArray a = new JSONArray(app.getSharedPreferences("mt", 0).getString("dl", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item();
                it.id = o.getLong("id");
                it.name = o.getString("name");
                it.url = o.getString("url");
                it.ua = o.optString("ua");
                it.uri = o.optString("uri");
                it.segs = o.optString("segs");
                it.ref = o.optString("ref");
                it.total = o.optLong("total");
                it.done = o.optLong("done");
                it.status = o.optInt("status");
                if (it.status == ACTIVE) it.status = PAUSED;
                items.add(it);
            }
        } catch (Exception e) { }
    }

    static synchronized void save() {
        try {
            JSONArray a = new JSONArray();
            for (Item it : items) {
                JSONObject o = new JSONObject();
                o.put("id", it.id);
                o.put("name", it.name);
                o.put("url", it.url);
                o.put("ua", it.ua);
                o.put("uri", it.uri);
                o.put("segs", it.segs);
                o.put("ref", it.ref);
                o.put("total", it.total);
                o.put("done", it.done);
                o.put("status", it.status);
                a.put(o);
            }
            app.getSharedPreferences("mt", 0).edit().putString("dl", a.toString()).apply();
        } catch (Exception e) { }
    }

    public static synchronized ArrayList<Item> snapshot() { return new ArrayList<>(items); }

    static File dir() {
        File d = new File(app.getExternalFilesDir(null), "tmp");
        d.mkdirs();
        return d;
    }

    static String clean(String n) {
        if (n == null || n.trim().isEmpty()) n = "download";
        return n.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static void start(Context c, String url, String name, String ua) {
        init(c);
        if (Build.VERSION.SDK_INT >= 33 && c instanceof Activity
                && c.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            ((Activity) c).requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 5);
        Item it = new Item();
        it.id = System.currentTimeMillis();
        it.name = clean(name);
        it.url = url;
        it.ua = ua == null ? "" : ua;
        it.ref = refOf(c);
        synchronized (DownloadEngine.class) { items.add(0, it); }
        resume(it);
    }

    public static void resume(final Item it) {
        if (it.running) return;
        if (Prefs.wifiOnly(app) && !onWifi()) { it.status = PAUSED; it.err = "Wi-Fi chahiye"; save(); return; }
        it.stop = false;
        it.status = ACTIVE;
        it.err = "";
        it.running = true;
        save();
        try { app.startForegroundService(new Intent(app, DownloadService.class)); } catch (Exception e) { }
        new Thread(new Runnable() { public void run() { download(it); } }).start();
    }

    public static void pause(Item it) {
        it.stop = true;
        it.status = PAUSED;
        it.speed = 0;
        save();
    }

    public static void remove(Item it, boolean file) {
        it.stop = true;
        synchronized (DownloadEngine.class) { items.remove(it); }
        if (file && it.uri != null && !it.uri.isEmpty()) {
            try {
                if (it.uri.startsWith("file:")) new File(Uri.parse(it.uri).getPath()).delete();
                else app.getContentResolver().delete(Uri.parse(it.uri), null, null);
            } catch (Exception e) { }
        }
        new File(dir(), it.id + ".part").delete();
        deleteDir(new File(dir(), "h" + it.id));
        save();
    }

    public static void clearFinished() {
        synchronized (DownloadEngine.class) {
            for (int i = items.size() - 1; i >= 0; i--)
                if (items.get(i).status == DONE || items.get(i).status == FAILED) items.remove(i);
        }
        save();
    }

    static void downloadSingle(Item it) {
        HttpURLConnection c = null;
        try {
            File f = new File(dir(), it.id + ".part");
            long have = f.exists() ? f.length() : 0;
            c = (HttpURLConnection) new URL(it.url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(true);
            if (!it.ua.isEmpty()) c.setRequestProperty("User-Agent", it.ua);
            String ck = CookieManager.getInstance().getCookie(it.url);
            if (ck != null) c.setRequestProperty("Cookie", ck);
            try {
                URL u = new URL(it.url);
                refHdr(c, it, u);
            } catch (Exception e) { }
            if (have > 0) c.setRequestProperty("Range", "bytes=" + have + "-");
            int code = c.getResponseCode();
            if (code >= 400) throw new IOException("HTTP " + code);
            boolean append = code == 206 && have > 0;
            if (!append) have = 0;
            long len = c.getContentLengthLong();
            it.total = len > 0 ? len + have : 0;
            it.done = have;
            InputStream in = c.getInputStream();
            FileOutputStream out = new FileOutputStream(f, append);
            byte[] buf = new byte[65536];
            int n;
            long tw = System.currentTimeMillis(), bw = it.done, ts = tw;
            while (!it.stop && (n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                it.done += n;
                long now = System.currentTimeMillis();
                if (now - tw >= 1000) {
                    it.speed = (it.done - bw) * 1000.0 / (now - tw);
                    tw = now;
                    bw = it.done;
                    if (now - ts > 5000) { ts = now; save(); }
                }
            }
            out.close();
            in.close();
            if (it.stop) return;
            if (it.total > 0 && it.done < it.total) throw new IOException("Incomplete");
            it.total = it.done;
            publish(it, f);
            it.status = DONE;
        } catch (Exception e) {
            if (!it.stop) { it.status = FAILED; it.err = String.valueOf(e.getMessage()); }
        } finally {
            it.speed = 0;
            it.running = false;
            if (c != null) c.disconnect();
            save();
        }
    }

    static String mime(String name) {
        if (name.toLowerCase().endsWith(".ts")) return "video/mp2t";
        int i = name.lastIndexOf('.');
        String ext = i >= 0 ? name.substring(i + 1).toLowerCase() : "";
        String m = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return m == null ? "application/octet-stream" : m;
    }

    static void publish(Item it, File f) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            android.content.ContentResolver r = app.getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, it.name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, mime(it.name));
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MT Browser");
            v.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri u = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (u == null) throw new IOException("Save failed");
            copy(f, r.openOutputStream(u));
            v.clear();
            v.put(MediaStore.MediaColumns.IS_PENDING, 0);
            r.update(u, v, null, null);
            it.uri = u.toString();
        } else {
            File d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            d.mkdirs();
            File dst = new File(d, it.name);
            copy(f, new FileOutputStream(dst));
            it.uri = Uri.fromFile(dst).toString();
        }
        f.delete();
    }

    static void copy(File f, OutputStream os) throws IOException {
        InputStream in = new FileInputStream(f);
        byte[] b = new byte[65536];
        int n;
        while ((n = in.read(b)) > 0) os.write(b, 0, n);
        in.close();
        os.close();
    }

    static String sniff(byte[] b, int n) {
        if (n >= 4 && (b[0] & 0xFF) == 0x1A && (b[1] & 0xFF) == 0x45 && (b[2] & 0xFF) == 0xDF && (b[3] & 0xFF) == 0xA3) return "mkv";
        if (n >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') return "mp4";
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'A' && b[9] == 'V' && b[10] == 'I') return "avi";
        if (n >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "pdf";
        if (n >= 3 && b[0] == 'I' && b[1] == 'D' && b[2] == '3') return "mp3";
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (n >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (n >= 4 && b[0] == 'O' && b[1] == 'g' && b[2] == 'g' && b[3] == 'S') return "ogg";
        if (n >= 4 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4) return "zip";
        return null;
    }

    static boolean needsExt(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0) return true;
        String e = name.substring(i + 1).toLowerCase();
        return e.equals("bin") || !android.webkit.MimeTypeMap.getSingleton().hasExtension(e);
    }

    static String withExt(String name, String ext) {
        if (name.toLowerCase().endsWith(".bin")) name = name.substring(0, name.length() - 4);
        return name + "." + ext;
    }

    public static boolean rename(Item it, String nn) {
        try {
            nn = clean(nn);
            if (it.uri.startsWith("file:")) {
                File old = new File(Uri.parse(it.uri).getPath());
                File dst = new File(old.getParentFile(), nn);
                if (!old.renameTo(dst)) return false;
                it.uri = Uri.fromFile(dst).toString();
            } else {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, nn);
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime(nn));
                if (app.getContentResolver().update(Uri.parse(it.uri), v, null, null) < 1) return false;
            }
            it.name = nn;
            save();
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean fixExt(Item it) {
        try {
            InputStream in = app.getContentResolver().openInputStream(Uri.parse(it.uri));
            byte[] hb = new byte[16];
            int n = in.read(hb);
            in.close();
            String e = sniff(hb, Math.max(n, 0));
            if (e == null) return false;
            return rename(it, withExt(it.name, e));
        } catch (Exception ex) { return false; }
    }

    static class Seg { long s, e; volatile long c; String err = ""; }

    static boolean onWifi() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return nc != null && nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI);
        } catch (Exception e) { return true; }
    }

    static HttpURLConnection open(Item it, String range) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(it.url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(true);
        if (!it.ua.isEmpty()) c.setRequestProperty("User-Agent", it.ua);
        String ck = CookieManager.getInstance().getCookie(it.url);
        if (ck != null) c.setRequestProperty("Cookie", ck);
        try {
            URL u = new URL(it.url);
            refHdr(c, it, u);
        } catch (Exception e) { }
        if (range != null) c.setRequestProperty("Range", range);
        return c;
    }

    static long[] probe(Item it) {
        HttpURLConnection c = null;
        try {
            c = open(it, "bytes=0-0");
            int code = c.getResponseCode();
            String cr = c.getHeaderField("Content-Range");
            if (code == 206 && cr != null && cr.contains("/"))
                return new long[]{Long.parseLong(cr.substring(cr.indexOf('/') + 1).trim()), 1};
            return new long[]{c.getContentLengthLong(), 0};
        } catch (Exception e) {
            return new long[]{0, 0};
        } finally {
            if (c != null) c.disconnect();
        }
    }

    static void download(Item it) {
        if (it.url.toLowerCase().contains(".m3u8")) { downloadHls(it); return; }
        File f = new File(dir(), it.id + ".part");
        boolean resume = !it.segs.isEmpty() && f.exists();
        long total = it.total;
        boolean multi = resume;
        if (!resume) {
            if (f.exists() && f.length() > 0) { downloadSingle(it); return; }
            long[] p = probe(it);
            total = p[0];
            multi = p[1] == 1 && total >= 8L * 1024 * 1024;
        }
        if (!multi) { downloadSingle(it); return; }
        downloadMulti(it, f, total, resume);
    }

    static String ser(ArrayList<Seg> l) {
        StringBuilder b = new StringBuilder();
        for (Seg g : l) {
            if (b.length() > 0) b.append(';');
            b.append(g.s).append(':').append(g.c).append(':').append(g.e);
        }
        return b.toString();
    }

    static void runSeg(Item it, File f, Seg g) {
        int tries = 0;
        while (!it.stop && g.c <= g.e && tries < 5) {
            HttpURLConnection c = null;
            try {
                c = open(it, "bytes=" + g.c + "-" + g.e);
                if (c.getResponseCode() != 206) throw new IOException("HTTP " + c.getResponseCode());
                InputStream in = c.getInputStream();
                RandomAccessFile r = new RandomAccessFile(f, "rw");
                r.seek(g.c);
                byte[] buf = new byte[65536];
                int n;
                while (!it.stop && g.c <= g.e && (n = in.read(buf, 0, (int) Math.min(buf.length, g.e - g.c + 1))) > 0) {
                    r.write(buf, 0, n);
                    g.c += n;
                }
                r.close();
                in.close();
                if (g.c <= g.e && !it.stop) tries++; else tries = 0;
            } catch (Exception e) {
                g.err = String.valueOf(e.getMessage());
                tries++;
                try { Thread.sleep(1000); } catch (Exception x) { }
            } finally {
                if (c != null) c.disconnect();
            }
        }
    }

    static void downloadMulti(final Item it, final File f, long total, boolean resume) {
        final ArrayList<Seg> segs = new ArrayList<>();
        ArrayList<Thread> th = new ArrayList<>();
        try {
            it.total = total;
            if (resume) {
                for (String p : it.segs.split(";")) {
                    String[] x = p.split(":");
                    Seg g = new Seg();
                    g.s = Long.parseLong(x[0]);
                    g.c = Long.parseLong(x[1]);
                    g.e = Long.parseLong(x[2]);
                    segs.add(g);
                }
            } else {
                RandomAccessFile r = new RandomAccessFile(f, "rw");
                r.setLength(total);
                r.close();
                int n = total > 200L * 1024 * 1024 ? 8 : 6;
                long part = total / n;
                for (int i = 0; i < n; i++) {
                    Seg g = new Seg();
                    g.s = i * part;
                    g.c = g.s;
                    g.e = i == n - 1 ? total - 1 : (i + 1) * part - 1;
                    segs.add(g);
                }
            }
            it.segs = ser(segs);
            save();
            for (final Seg g : segs) {
                if (g.c > g.e) continue;
                Thread t = new Thread(new Runnable() { public void run() { runSeg(it, f, g); } });
                t.start();
                th.add(t);
            }
            long tw = System.currentTimeMillis(), ts = tw, bw = 0;
            for (Seg g : segs) bw += g.c - g.s;
            while (true) {
                Thread.sleep(500);
                long sum = 0;
                for (Seg g : segs) sum += Math.min(g.c, g.e + 1) - g.s;
                it.done = sum;
                long now = System.currentTimeMillis();
                if (now - tw >= 1000) { it.speed = (sum - bw) * 1000.0 / (now - tw); tw = now; bw = sum; }
                if (now - ts > 5000) { ts = now; it.segs = ser(segs); save(); }
                boolean alive = false;
                for (Thread t : th) if (t.isAlive()) alive = true;
                if (!alive) break;
            }
            it.segs = ser(segs);
            if (it.stop) return;
            boolean ok = true;
            String err = "";
            for (Seg g : segs) if (g.c <= g.e) { ok = false; if (!g.err.isEmpty()) err = g.err; }
            if (!ok) throw new IOException(err.isEmpty() ? "Incomplete" : err);
            it.done = total;
            it.total = total;
            it.segs = "";
            publish(it, f);
            it.status = DONE;
        } catch (Exception e) {
            it.stop = true;
            if (it.status != PAUSED) { it.status = FAILED; it.err = String.valueOf(e.getMessage()); }
        } finally {
            it.speed = 0;
            it.running = false;
            save();
        }
    }

    static void refHdr(HttpURLConnection c, Item it, URL u) {
        String r = (it.ref != null && it.ref.startsWith("http")) ? it.ref : u.getProtocol() + "://" + u.getHost() + "/";
        c.setRequestProperty("Referer", r);
        try {
            URL ru = new URL(r);
            c.setRequestProperty("Origin", ru.getProtocol() + "://" + ru.getHost());
        } catch (Exception e) { }
    }

    static String refOf(Context c) {
        try {
            if (c instanceof MainActivity) {
                MainActivity m = (MainActivity) c;
                if (m.cur != null && m.cur.wv.getUrl() != null) return m.cur.wv.getUrl();
            }
            if (c instanceof PlayerActivity) {
                String p = ((PlayerActivity) c).page;
                if (p != null) return p;
            }
        } catch (Throwable e) { }
        return "";
    }

    static void deleteDir(File d) {
        File[] fs = d.listFiles();
        if (fs != null) for (File x : fs) x.delete();
        d.delete();
    }

    static String resolve(String base, String rel) {
        try { return new URL(new URL(base), rel).toString(); } catch (Exception e) { return rel; }
    }

    static String attr(String line, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:^|[:,])" + name + "=(\"([^\"]*)\"|[^,]*)").matcher(line);
        if (!m.find()) return null;
        return m.group(2) != null ? m.group(2) : m.group(1);
    }

    static byte[] getBytes(Item it, String u, java.util.concurrent.atomic.AtomicLong cnt) throws IOException {
        Item t = new Item();
        t.url = u;
        t.ua = it.ua;
        t.ref = it.ref;
        HttpURLConnection c = open(t, null);
        try {
            int code = c.getResponseCode();
            if (code >= 400) throw new IOException("HTTP " + code);
            InputStream in = c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[32768];
            int n;
            while (!it.stop && (n = in.read(b)) > 0) { bo.write(b, 0, n); if (cnt != null) cnt.addAndGet(n); }
            in.close();
            if (it.stop) throw new IOException("stopped");
            return bo.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    static void downloadHls(final Item it) {
        final File sd = new File(dir(), "h" + it.id);
        sd.mkdirs();
        try {
            String base = it.url;
            String pl = new String(getBytes(it, base, null), "UTF-8");
            if (pl.contains("#EXT-X-STREAM-INF")) {
                String[] ls = pl.split("\\r?\\n");
                long best = -1;
                String bu = null;
                for (int i = 0; i < ls.length; i++) {
                    if (!ls[i].startsWith("#EXT-X-STREAM-INF")) continue;
                    long bw = 0;
                    String bs = attr(ls[i], "BANDWIDTH");
                    if (bs != null) { try { bw = Long.parseLong(bs.trim()); } catch (Exception e) { } }
                    for (int j = i + 1; j < ls.length; j++) {
                        String s = ls[j].trim();
                        if (s.isEmpty() || s.startsWith("#")) continue;
                        if (bw > best) { best = bw; bu = resolve(base, s); }
                        break;
                    }
                }
                if (bu == null) throw new IOException("Playlist samajh nahi aaya");
                base = bu;
                pl = new String(getBytes(it, base, null), "UTF-8");
            }
            final ArrayList<String> segs = new ArrayList<>(), sk = new ArrayList<>(), si = new ArrayList<>();
            String keyUri = null, iv = null, method = "NONE", map = null;
            long seq = 0;
            for (String raw : pl.split("\\r?\\n")) {
                String s = raw.trim();
                if (s.isEmpty()) continue;
                if (s.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                    try { seq = Long.parseLong(s.substring(22).trim()); } catch (Exception e) { }
                } else if (s.startsWith("#EXT-X-KEY:")) {
                    method = attr(s, "METHOD");
                    if (method == null) method = "NONE";
                    String ku = attr(s, "URI");
                    keyUri = ku == null ? null : resolve(base, ku);
                    iv = attr(s, "IV");
                } else if (s.startsWith("#EXT-X-MAP:")) {
                    String mu = attr(s, "URI");
                    if (mu != null) map = resolve(base, mu);
                } else if (!s.startsWith("#")) {
                    if (!method.equals("NONE") && !method.equals("AES-128")) throw new IOException("Encrypted (DRM) stream, download nahi ho sakta");
                    segs.add(resolve(base, s));
                    sk.add(method.equals("AES-128") ? keyUri : null);
                    si.add(iv);
                }
            }
            if (segs.isEmpty()) throw new IOException("Playlist khali hai");
            final long seq0 = seq;
            final String map0 = map;
            final int N = segs.size();
            String nm = it.name;
            int dd = nm.lastIndexOf('.');
            if (dd > 0 && nm.length() - dd <= 6) nm = nm.substring(0, dd);
            it.name = nm + (map0 != null ? ".mp4" : ".ts");
            final java.util.concurrent.atomic.AtomicLong bytes = new java.util.concurrent.atomic.AtomicLong(0);
            final java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger(0);
            final java.util.concurrent.atomic.AtomicInteger fin = new java.util.concurrent.atomic.AtomicInteger(0);
            final java.util.HashMap<String, byte[]> keys = new java.util.HashMap<>();
            final String[] fail = {null};
            for (int i = 0; i < N; i++) {
                File f = new File(sd, "s" + i);
                if (f.exists() && f.length() > 0) { bytes.addAndGet(f.length()); fin.incrementAndGet(); }
            }
            ArrayList<Thread> th = new ArrayList<>();
            for (int w = 0; w < 6; w++) {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        while (!it.stop && fail[0] == null) {
                            int i = next.getAndIncrement();
                            if (i >= N) return;
                            File f = new File(sd, "s" + i);
                            if (f.exists() && f.length() > 0) continue;
                            int tries = 0;
                            while (!it.stop && tries < 4) {
                                try {
                                    byte[] data = getBytes(it, segs.get(i), new java.util.concurrent.atomic.AtomicLong(0));
                                    String ku = sk.get(i);
                                    if (ku != null) {
                                        byte[] key;
                                        synchronized (keys) { key = keys.get(ku); }
                                        if (key == null) {
                                            key = getBytes(it, ku, null);
                                            synchronized (keys) { keys.put(ku, key); }
                                        }
                                        byte[] ivb = new byte[16];
                                        String ih = si.get(i);
                                        if (ih != null) {
                                            String hx = (ih.startsWith("0x") || ih.startsWith("0X")) ? ih.substring(2) : ih;
                                            while (hx.length() < 32) hx = "0" + hx;
                                            for (int k = 0; k < 16; k++) ivb[k] = (byte) Integer.parseInt(hx.substring(k * 2, k * 2 + 2), 16);
                                        } else {
                                            long sq = seq0 + i;
                                            for (int k = 15; k >= 8; k--) { ivb[k] = (byte) (sq & 0xFF); sq >>= 8; }
                                        }
                                        javax.crypto.Cipher ci = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
                                        ci.init(javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "AES"), new javax.crypto.spec.IvParameterSpec(ivb));
                                        data = ci.doFinal(data);
                                    }
                                    File tmp = new File(sd, "s" + i + ".tmp");
                                    FileOutputStream fo = new FileOutputStream(tmp);
                                    fo.write(data);
                                    fo.close();
                                    if (!tmp.renameTo(f)) throw new IOException("Rename failed");
                                    bytes.addAndGet(data.length);
                                    fin.incrementAndGet();
                                    break;
                                } catch (Exception e) {
                                    tries++;
                                    if (tries >= 4 && !it.stop) fail[0] = String.valueOf(e.getMessage());
                                    try { Thread.sleep(800); } catch (Exception x) { }
                                }
                            }
                        }
                    }
                });
                t.start();
                th.add(t);
            }
            long tw = System.currentTimeMillis(), ts = tw, bw0 = bytes.get();
            while (true) {
                Thread.sleep(500);
                long bb = bytes.get();
                int f2 = fin.get();
                it.done = bb;
                it.total = f2 > 0 ? bb * N / f2 : 0;
                long now = System.currentTimeMillis();
                if (now - tw >= 1000) { it.speed = (bb - bw0) * 1000.0 / (now - tw); tw = now; bw0 = bb; }
                if (now - ts > 5000) { ts = now; save(); }
                boolean alive = false;
                for (Thread x : th) if (x.isAlive()) alive = true;
                if (!alive) break;
            }
            if (it.stop) return;
            if (fail[0] != null) throw new IOException(fail[0]);
            for (int i = 0; i < N; i++) if (!new File(sd, "s" + i).exists()) throw new IOException("Segment missing");
            File out = new File(dir(), it.id + ".part");
            FileOutputStream fo = new FileOutputStream(out);
            if (map0 != null) fo.write(getBytes(it, map0, null));
            byte[] buf = new byte[65536];
            for (int i = 0; i < N; i++) {
                FileInputStream fi = new FileInputStream(new File(sd, "s" + i));
                int n;
                while ((n = fi.read(buf)) > 0) fo.write(buf, 0, n);
                fi.close();
            }
            fo.close();
            deleteDir(sd);
            it.done = out.length();
            it.total = it.done;
            publish(it, out);
            it.status = DONE;
        } catch (Exception e) {
            if (!it.stop) { it.status = FAILED; it.err = String.valueOf(e.getMessage()); }
        } finally {
            it.speed = 0;
            it.running = false;
            save();
        }
    }
}
