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
        synchronized (DownloadEngine.class) { items.add(0, it); }
        resume(it);
    }

    public static void resume(final Item it) {
        if (it.running) return;
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
        save();
    }

    public static void clearFinished() {
        synchronized (DownloadEngine.class) {
            for (int i = items.size() - 1; i >= 0; i--)
                if (items.get(i).status == DONE || items.get(i).status == FAILED) items.remove(i);
        }
        save();
    }

    static void download(Item it) {
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
                c.setRequestProperty("Referer", u.getProtocol() + "://" + u.getHost() + "/");
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
}
