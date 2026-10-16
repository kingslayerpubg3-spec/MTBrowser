package com.mt.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

public class FastDownload {
    static int dp(Activity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    static TextView tv(Activity a, String t, int sp, int col, boolean bold) {
        TextView v = new TextView(a);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(col);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    static String fmt(long ms) {
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    public static void show(final Activity a, final String url, String name0, final String ua) {
        final BottomSheetDialog d = new BottomSheetDialog(a);
        final String[] name = {DownloadEngine.clean(name0)};
        final int tx = UiTheme.tx(a), sub = UiTheme.sub(a), acc = UiTheme.acc(a);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a, 18), dp(a, 14), dp(a, 18), dp(a, 24));
        GradientDrawable rb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{UiTheme.bg1(a), UiTheme.bg2(a)});
        float cr = dp(a, 28);
        rb.setCornerRadii(new float[]{cr, cr, cr, cr, 0, 0, 0, 0});
        root.setBackground(rb);

        FrameLayout top = new FrameLayout(a);
        TextView title = tv(a, "Fast Download", 22, tx, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        ImageView x = new ImageView(a);
        x.setImageResource(R.drawable.ic_m_close);
        x.setColorFilter(sub);
        x.setPadding(dp(a, 9), dp(a, 9), dp(a, 9), dp(a, 9));
        x.setOnClickListener(v -> d.dismiss());
        top.addView(x, new FrameLayout.LayoutParams(dp(a, 40), dp(a, 40), Gravity.END | Gravity.TOP));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(a, 44)));

        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(a, 16), 0, dp(a, 20));
        FrameLayout th = new FrameLayout(a);
        GradientDrawable tg = new GradientDrawable();
        tg.setColor(UiTheme.chip(a));
        tg.setCornerRadius(dp(a, 14));
        th.setBackground(tg);
        th.setClipToOutline(true);
        final ImageView img = new ImageView(a);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        th.addView(img, new FrameLayout.LayoutParams(-1, -1));
        boolean vid = name[0].toLowerCase().matches(".*\\.(mp4|mkv|webm|3gp|avi|mov|m4v)$");
        ImageView badge = new ImageView(a);
        badge.setImageResource(vid ? R.drawable.ic_m_play : R.drawable.ic_m_file);
        badge.setColorFilter(Color.WHITE);
        badge.setPadding(dp(a, 9), dp(a, 9), dp(a, 9), dp(a, 9));
        GradientDrawable bg2 = new GradientDrawable();
        bg2.setShape(GradientDrawable.OVAL);
        bg2.setColor(vid ? 0xCCEF3B2D : acc);
        badge.setBackground(bg2);
        th.addView(badge, new FrameLayout.LayoutParams(dp(a, 40), dp(a, 40), Gravity.CENTER));
        row.addView(th, new LinearLayout.LayoutParams(dp(a, 120), dp(a, 78)));

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout nr = new LinearLayout(a);
        nr.setGravity(Gravity.CENTER_VERTICAL);
        final TextView nm = tv(a, name[0], 15, tx, true);
        nm.setMaxLines(3);
        nm.setEllipsize(TextUtils.TruncateAt.END);
        nr.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
        ImageView pen = new ImageView(a);
        pen.setImageResource(R.drawable.ic_m_edit);
        pen.setColorFilter(sub);
        pen.setPadding(dp(a, 7), dp(a, 7), dp(a, 7), dp(a, 7));
        pen.setOnClickListener(v -> {
            final EditText e = new EditText(a);
            e.setText(name[0]);
            new AlertDialog.Builder(a).setTitle("File name").setView(e)
                .setPositiveButton("OK", (dd, i) -> {
                    String s = e.getText().toString().trim();
                    if (!s.isEmpty()) { name[0] = DownloadEngine.clean(s); nm.setText(name[0]); }
                }).setNegativeButton("Cancel", null).show();
        });
        nr.addView(pen, new LinearLayout.LayoutParams(dp(a, 32), dp(a, 32)));
        col.addView(nr);
        final TextView info = tv(a, "Checking...", 13, sub, false);
        info.setPadding(0, dp(a, 6), 0, 0);
        col.addView(info);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
        cp.setMargins(dp(a, 14), 0, 0, 0);
        row.addView(col, cp);
        root.addView(row);

        TextView go = tv(a, "Download to device", 17, Color.WHITE, true);
        go.setGravity(Gravity.CENTER);
        GradientDrawable gg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{acc, UiTheme.acc2(a)});
        gg.setCornerRadius(dp(a, 28));
        go.setBackground(gg);
        go.setOnClickListener(v -> {
            d.dismiss();
            DownloadEngine.start(a, url, name[0], ua);
            Toast.makeText(a, "Download shuru ho gaya", Toast.LENGTH_SHORT).show();
        });
        root.addView(go, new LinearLayout.LayoutParams(-1, dp(a, 54)));

        TextView wl = tv(a, "Watch later", 15, acc, true);
        wl.setGravity(Gravity.CENTER);
        wl.setPadding(0, dp(a, 14), 0, 0);
        wl.setOnClickListener(v -> {
            Store.addTo(a, "wl", name[0], url, 200);
            Toast.makeText(a, "Watch Later me add ho gaya", Toast.LENGTH_SHORT).show();
            d.dismiss();
        });
        root.addView(wl, new LinearLayout.LayoutParams(-1, -2));

        d.setContentView(root);
        ((View) root.getParent()).setBackgroundColor(Color.TRANSPARENT);
        d.show();
        d.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        final boolean media = name[0].toLowerCase().matches(".*\\.(mp4|mkv|webm|3gp|avi|mov|m4v|mp3|m4a|aac|wav|ogg)$");
        new Thread(() -> {
            if (url.toLowerCase().contains(".m3u8")) { a.runOnUiThread(() -> info.setText("HD stream (HLS) • merge hokar save hoga")); return; }
            long len = -1;
            String dur = "";
            Bitmap bmp = null;
            String ck = CookieManager.getInstance().getCookie(url);
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Range", "bytes=0-0");
                if (ua != null && !ua.isEmpty()) c.setRequestProperty("User-Agent", ua);
                if (ck != null) c.setRequestProperty("Cookie", ck);
                int code = c.getResponseCode();
                String cr2 = c.getHeaderField("Content-Range");
                if (code == 206 && cr2 != null && cr2.contains("/")) len = Long.parseLong(cr2.substring(cr2.indexOf('/') + 1).trim());
                else len = c.getContentLengthLong();
                c.disconnect();
            } catch (Throwable e) { }
            if (media) {
                try {
                    MediaMetadataRetriever r = new MediaMetadataRetriever();
                    HashMap<String, String> hh = new HashMap<>();
                    if (ua != null && !ua.isEmpty()) hh.put("User-Agent", ua);
                    if (ck != null) hh.put("Cookie", ck);
                    r.setDataSource(url, hh);
                    String ds = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (ds != null) dur = fmt(Long.parseLong(ds));
                    bmp = Build.VERSION.SDK_INT >= 27
                        ? r.getScaledFrameAtTime(2000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 400, 260)
                        : r.getFrameAtTime(2000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    r.release();
                } catch (Throwable e) { }
            }
            final String t = (dur.isEmpty() ? "" : dur + "   ") + (len > 0 ? DownloadsActivity.size(len) : "");
            final Bitmap fb = bmp;
            a.runOnUiThread(() -> {
                if (!d.isShowing()) return;
                info.setText(t.isEmpty() ? "Size pata nahi" : t);
                if (fb != null) img.setImageBitmap(fb);
            });
        }).start();
    }
}
