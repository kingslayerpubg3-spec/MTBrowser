package com.mt.browser;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Qualities {
    public static class Q {
        public String label = "", note = "", url = "";
        public int height;
        public long bw, size;
        public boolean hls, auto, sepAudio, est;
    }

    public interface PlayCb { void play(Q q); }

    static int dp(Activity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    public static void setHeight(ExoPlayer p, int h) {
        if (p == null) return;
        TrackSelectionParameters cur = p.getTrackSelectionParameters();
        if (h <= 0) {
            p.setTrackSelectionParameters(cur.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO).build());
            return;
        }
        for (Tracks.Group g : p.getCurrentTracks().getGroups()) {
            if (g.getType() != C.TRACK_TYPE_VIDEO) continue;
            int best = -1, bd = Integer.MAX_VALUE;
            for (int i = 0; i < g.length; i++) {
                if (!g.isTrackSupported(i)) continue;
                int d = Math.abs(g.getTrackFormat(i).height - h);
                if (d < bd) { bd = d; best = i; }
            }
            if (best >= 0) {
                p.setTrackSelectionParameters(cur.buildUpon()
                        .setOverrideForType(new TrackSelectionOverride(g.getMediaTrackGroup(), best)).build());
                return;
            }
        }
    }

    static String nameFor(Q q, String title) {
        String t = (title == null ? "" : title).replaceAll("[\\\\/:*?\"<>|]", " ").trim();
        if (t.length() > 80) t = t.substring(0, 80);
        if (t.isEmpty()) t = "Video";
        if (q.hls) return t + " " + q.label + ".mp4";
        String g = URLUtil.guessFileName(q.url, null, null), l = g.toLowerCase();
        boolean generic = l.endsWith(".bin") || !g.contains(".") || l.startsWith("download") || l.startsWith("videoplayback");
        return generic ? t + " " + q.label + ".mp4" : g;
    }

    static ArrayList<Q> load(String url, List<String> alts, String ua, String ref, double dur) {
        ArrayList<Q> out = new ArrayList<>();
        String lu = url.toLowerCase();
        try {
            if (lu.contains(".m3u8")) {
                DownloadEngine.Item hit = new DownloadEngine.Item();
                hit.url = url;
                hit.ua = ua == null ? "" : ua;
                hit.ref = ref == null ? "" : ref;
                String pl = new String(DownloadEngine.getBytes(hit, url, null), "UTF-8");
                if (pl.contains("#EXT-X-STREAM-INF")) {
                    String[] ls = pl.split("\\r?\\n");
                    HashMap<Integer, Q> by = new HashMap<>();
                    for (int i = 0; i < ls.length; i++) {
                        if (!ls[i].startsWith("#EXT-X-STREAM-INF")) continue;
                        long bw = 0;
                        int hh = 0;
                        String bs = DownloadEngine.attr(ls[i], "BANDWIDTH");
                        if (bs != null) { try { bw = Long.parseLong(bs.trim()); } catch (Exception e) { } }
                        String rs = DownloadEngine.attr(ls[i], "RESOLUTION");
                        if (rs != null && rs.indexOf('x') > 0) {
                            try { hh = Integer.parseInt(rs.substring(rs.indexOf('x') + 1).trim()); } catch (Exception e) { }
                        }
                        String u = null;
                        for (int j = i + 1; j < ls.length; j++) {
                            String s = ls[j].trim();
                            if (s.isEmpty() || s.startsWith("#")) continue;
                            u = DownloadEngine.resolve(url, s);
                            break;
                        }
                        if (u == null) continue;
                        int key = hh > 0 ? hh : -(int) (bw / 1000) - 1;
                        Q ex = by.get(key);
                        if (ex != null && ex.bw >= bw) continue;
                        Q q = new Q();
                        q.hls = true;
                        q.height = hh;
                        q.bw = bw;
                        q.url = u;
                        q.sepAudio = DownloadEngine.attr(ls[i], "AUDIO") != null;
                        q.label = hh > 0 ? hh + "p" : (bw / 1000) + " kbps";
                        q.note = hh >= 2160 ? "4K" : hh >= 1440 ? "2K" : hh >= 1080 ? "Full HD" : hh >= 720 ? "HD" : hh > 0 ? "SD" : "";
                        if (bw > 0 && dur > 0) { q.size = (long) (bw * dur / 8); q.est = true; }
                        by.put(key, q);
                    }
                    out.addAll(by.values());
                    Collections.sort(out, new Comparator<Q>() {
                        public int compare(Q x, Q y) {
                            if (x.height != y.height) return y.height - x.height;
                            return Long.compare(y.bw, x.bw);
                        }
                    });
                }
            } else {
                ArrayList<String> us = new ArrayList<>();
                us.add(url);
                if (alts != null) for (String s : alts)
                    if (s != null && !us.contains(s) && !s.toLowerCase().contains(".m3u8") && us.size() < 6) us.add(s);
                java.util.regex.Pattern pt = java.util.regex.Pattern.compile("(?i)(\\d{3,4})p");
                for (int i = 0; i < us.size(); i++) {
                    DownloadEngine.Item pi = new DownloadEngine.Item();
                    pi.url = us.get(i);
                    pi.ua = ua == null ? "" : ua;
                    pi.ref = ref == null ? "" : ref;
                    long[] pr = DownloadEngine.probe(pi);
                    Q q = new Q();
                    q.url = pi.url;
                    q.size = Math.max(0, pr[0]);
                    java.util.regex.Matcher m = pt.matcher(q.url);
                    if (m.find()) {
                        q.height = Integer.parseInt(m.group(1));
                        q.label = q.height + "p";
                    } else q.label = us.size() > 1 ? "File " + (i + 1) : "Original";
                    out.add(q);
                }
                Collections.sort(out, new Comparator<Q>() {
                    public int compare(Q x, Q y) { return Long.compare(y.size, x.size); }
                });
                if (out.size() > 1) out.get(0).note = "Best";
            }
        } catch (Exception e) { }
        if (out.isEmpty()) {
            Q q = new Q();
            q.url = url;
            q.label = "Original";
            q.hls = lu.contains(".m3u8");
            out.add(q);
        }
        return out;
    }

    static TextView tv(Activity a, String t, int sp, int col, boolean bold) {
        TextView v = new TextView(a);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(col);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    static ImageView circle(Activity a, int res, int bgc, int tint) {
        ImageView v = new ImageView(a);
        v.setImageResource(res);
        v.setColorFilter(tint);
        int p = dp(a, 11);
        v.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(bgc);
        v.setBackground(g);
        return v;
    }

    public static void show(final Activity a, final String url, final List<String> alts, final String ua, final String ref,
                            final double dur, final String title, final ExoPlayer player, final PlayCb cb) {
        final BottomSheetDialog d = new BottomSheetDialog(a);
        int tx = UiTheme.tx(a), sub = UiTheme.sub(a);
        final LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a, 18), dp(a, 12), dp(a, 18), dp(a, 22));
        GradientDrawable rb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{UiTheme.bg1(a), UiTheme.bg2(a)});
        float cr = dp(a, 28);
        rb.setCornerRadii(new float[]{cr, cr, cr, cr, 0, 0, 0, 0});
        root.setBackground(rb);

        View handle = new View(a);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(UiTheme.stroke(a));
        hg.setCornerRadius(dp(a, 3));
        handle.setBackground(hg);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(a, 40), dp(a, 4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = dp(a, 14);
        root.addView(handle, hp);

        root.addView(tv(a, "Quality", 22, tx, true));
        TextView st = tv(a, title == null || title.isEmpty() ? "Video" : title, 13, sub, false);
        st.setSingleLine(true);
        st.setEllipsize(android.text.TextUtils.TruncateAt.END);
        st.setPadding(0, dp(a, 2), 0, dp(a, 12));
        root.addView(st);

        final LinearLayout wait = new LinearLayout(a);
        wait.setGravity(Gravity.CENTER_VERTICAL);
        wait.setPadding(0, dp(a, 14), 0, dp(a, 14));
        wait.addView(new ProgressBar(a), new LinearLayout.LayoutParams(dp(a, 26), dp(a, 26)));
        TextView wt = tv(a, "Qualities check ho rahi hain...", 14, sub, false);
        wt.setPadding(dp(a, 12), 0, 0, 0);
        wait.addView(wt);
        root.addView(wait);

        final LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(box);
        root.addView(sv);

        d.setContentView(root);
        ((View) root.getParent()).setBackgroundColor(Color.TRANSPARENT);
        d.show();
        d.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        new Thread(() -> {
            final ArrayList<Q> list = load(url, alts, ua, ref, dur);
            a.runOnUiThread(() -> {
                if (!d.isShowing()) return;
                if (list.size() == 1) {
                    d.dismiss();
                    Q q = list.get(0);
                    FastDownload.show(a, q.url, nameFor(q, title), ua);
                    return;
                }
                root.removeView(wait);
                fill(a, d, box, list, url, ua, title, player, cb);
            });
        }).start();
    }

    static void fill(final Activity a, final BottomSheetDialog d, LinearLayout box, ArrayList<Q> list,
                     String url, final String ua, final String title, ExoPlayer player, final PlayCb cb) {
        int tx = UiTheme.tx(a), sub = UiTheme.sub(a), acc = UiTheme.acc(a);
        Format vf = player == null ? null : player.getVideoFormat();
        boolean multi = false;
        for (Q q : list) if (q.hls && q.height > 0) multi = true;
        if (multi && player != null) {
            Q au = new Q();
            au.auto = true;
            au.hls = true;
            au.label = "Auto";
            au.note = "Network ke hisaab se best quality";
            list.add(0, au);
        }
        for (int i = 0; i < list.size(); i++) {
            final Q q = list.get(i);
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(a, 14), dp(a, 11), dp(a, 10), dp(a, 11));
            GradientDrawable g = new GradientDrawable();
            g.setColor(UiTheme.card(a));
            g.setCornerRadius(dp(a, 18));
            g.setStroke(dp(a, 1), UiTheme.stroke(a));
            row.setBackground(g);

            LinearLayout col = new LinearLayout(a);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(tv(a, q.label + (!q.auto && !q.note.isEmpty() ? "   " + q.note : ""), 17, tx, true));
            StringBuilder sb = new StringBuilder();
            if (q.auto) sb.append(q.note);
            else {
                sb.append(q.size > 0 ? (q.est ? "~" : "") + DownloadsActivity.size(q.size) : (q.hls ? "HD stream" : "Size pata nahi"));
                if (q.sepAudio) sb.append("  •  awaaz alag stream me");
            }
            boolean playing = !q.auto && ((q.hls && vf != null && q.height > 0 && q.height == vf.height) || (!q.hls && q.url.equals(url)));
            if (playing) sb.append("  •  Playing");
            TextView sTv = tv(a, sb.toString(), 12, sub, false);
            sTv.setPadding(0, dp(a, 3), 0, 0);
            col.addView(sTv);
            row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));

            if (cb != null) {
                ImageView pl = circle(a, R.drawable.ic_m_play, acc, Color.WHITE);
                pl.setOnClickListener(v -> { d.dismiss(); cb.play(q); });
                LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(a, 44), dp(a, 44));
                pp.setMargins(dp(a, 8), 0, 0, 0);
                row.addView(pl, pp);
            }
            if (!q.auto) {
                ImageView dl = circle(a, R.drawable.ic_m_download, UiTheme.chip(a), acc);
                dl.setOnClickListener(v -> { d.dismiss(); FastDownload.show(a, q.url, nameFor(q, title), ua); });
                LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(dp(a, 44), dp(a, 44));
                dp2.setMargins(dp(a, 8), 0, 0, 0);
                row.addView(dl, dp2);
            }
            row.setAlpha(0f);
            row.animate().alpha(1f).setStartDelay(i * 40L).setDuration(240).start();
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.bottomMargin = dp(a, 8);
            box.addView(row, rp);
        }
    }
}
