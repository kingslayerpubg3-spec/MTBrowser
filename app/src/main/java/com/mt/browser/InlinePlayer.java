package com.mt.browser;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.CookieManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class InlinePlayer {
    final MainActivity a;
    final FrameLayout host;
    FrameLayout ov;
    PlayerView pv;
    ExoPlayer player;
    ImageView playBtn, closeBtn, dlBtn, fullBtn;
    ProgressBar spin;
    SeekBar seek;
    TextView curT, durT;
    LinearLayout bar;
    MainActivity.Tab tab;
    ArrayList<String> cands = new ArrayList<>();
    int ci;
    double expDur;
    String title = "", ua = "", page = "";
    boolean active, shown = true, dragging, verified, wentFull, wasPlaying, advancing;
    long pos0;
    float fx, fy;
    boolean fwd, tapOk;
    final Handler h = new Handler(Looper.getMainLooper());
    final Runnable upd = new Runnable() { public void run() { update(); h.postDelayed(this, 250); } };
    final Runnable hideR = new Runnable() { public void run() { if (!dragging) setShown(false); } };

    InlinePlayer(MainActivity a, FrameLayout host) { this.a = a; this.host = host; }

    int dp(int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    static String fmt(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000, hh = s / 3600, m = (s % 3600) / 60, ss = s % 60;
        return hh > 0 ? String.format(Locale.US, "%d:%02d:%02d", hh, m, ss) : String.format(Locale.US, "%d:%02d", m, ss);
    }

    ImageView icon(int res, int pad, boolean tint) {
        ImageView v = new ImageView(a);
        v.setImageResource(res);
        if (tint) v.setColorFilter(Color.WHITE);
        v.setPadding(dp(pad), dp(pad), dp(pad), dp(pad));
        return v;
    }

    GradientDrawable circle(int c) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(c);
        return g;
    }

    TextView label() {
        TextView t = new TextView(a);
        t.setText("0:00");
        t.setTextColor(Color.WHITE);
        t.setTextSize(12);
        t.setShadowLayer(4, 0, 1, 0xAA000000);
        return t;
    }

    void build() {
        ov = new FrameLayout(a);
        ov.setBackgroundColor(Color.BLACK);
        ov.setClipChildren(true);
        pv = new PlayerView(a);
        pv.setUseController(false);
        pv.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        ov.addView(pv, new FrameLayout.LayoutParams(-1, -1));
        View touch = new View(a);
        ov.addView(touch, new FrameLayout.LayoutParams(-1, -1));

        spin = new ProgressBar(a);
        spin.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        ov.addView(spin, new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));

        playBtn = icon(R.drawable.ic_m_pause, 12, true);
        playBtn.setBackground(circle(0x66000000));
        ov.addView(playBtn, new FrameLayout.LayoutParams(dp(50), dp(50), Gravity.CENTER));

        closeBtn = icon(R.drawable.ic_m_close, 7, true);
        closeBtn.setBackground(circle(0x66000000));
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.TOP | Gravity.END);
        cl.setMargins(0, dp(6), dp(6), 0);
        ov.addView(closeBtn, cl);

        bar = new LinearLayout(a);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(16), dp(2), dp(0));
        bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xAA000000, 0x00000000}));
        curT = label();
        bar.addView(curT, new LinearLayout.LayoutParams(-2, -2));
        seek = new SeekBar(a);
        seek.setMax(10000);
        seek.setProgressDrawable(a.getDrawable(R.drawable.p_seek));
        seek.setThumb(a.getDrawable(R.drawable.p_thumb));
        seek.setSplitTrack(false);
        seek.setPadding(dp(10), 0, dp(10), 0);
        bar.addView(seek, new LinearLayout.LayoutParams(0, dp(28), 1f));
        durT = label();
        bar.addView(durT, new LinearLayout.LayoutParams(-2, -2));
        dlBtn = icon(R.drawable.ic_dl_line, 8, false);
        bar.addView(dlBtn, new LinearLayout.LayoutParams(dp(38), dp(38)));
        fullBtn = icon(R.drawable.ic_p_full, 8, true);
        bar.addView(fullBtn, new LinearLayout.LayoutParams(dp(38), dp(38)));
        ov.addView(bar, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        host.addView(ov, new FrameLayout.LayoutParams(dp(10), dp(10)));
        wire(touch);
    }

    void setShown(boolean s) {
        shown = s;
        final View[] vs = {bar, playBtn, closeBtn};
        for (final View v : vs) {
            if (s) v.setVisibility(View.VISIBLE);
            v.animate().alpha(s ? 1f : 0f).setDuration(180).withEndAction(() -> { if (!shown) v.setVisibility(View.GONE); }).start();
        }
    }

    void sched() {
        h.removeCallbacks(hideR);
        if (player != null && player.isPlaying()) h.postDelayed(hideR, 3500);
    }

    void wire(View touch) {
        playBtn.setOnClickListener(v -> { togglePlay(); sched(); });
        closeBtn.setOnClickListener(v -> stop(true));
        dlBtn.setOnClickListener(v -> download());
        fullBtn.setOnClickListener(v -> expand());
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                if (player == null) return;
                long d = player.getDuration();
                if (u && d > 0) curT.setText(fmt(d * p / 10000));
            }
            public void onStartTrackingTouch(SeekBar s) { dragging = true; h.removeCallbacks(hideR); }
            public void onStopTrackingTouch(SeekBar s) {
                if (player != null) { long d = player.getDuration(); if (d > 0) player.seekTo(d * s.getProgress() / 10000); }
                dragging = false;
                sched();
            }
        });
        final int slop = ViewConfiguration.get(a).getScaledTouchSlop();
        touch.setOnTouchListener((v, e) -> {
            int ac = e.getActionMasked();
            if (ac == MotionEvent.ACTION_DOWN) { fx = e.getX(); fy = e.getY(); fwd = false; tapOk = true; return true; }
            if (tab == null || ov == null) return true;
            if (ac == MotionEvent.ACTION_MOVE && !fwd && (Math.abs(e.getY() - fy) > slop || Math.abs(e.getX() - fx) > slop)) {
                fwd = true;
                tapOk = false;
                MotionEvent d = MotionEvent.obtain(e);
                d.setAction(MotionEvent.ACTION_DOWN);
                d.offsetLocation(ov.getX(), ov.getY());
                tab.wv.dispatchTouchEvent(d);
                d.recycle();
            }
            if (fwd) {
                MotionEvent m = MotionEvent.obtain(e);
                m.offsetLocation(ov.getX(), ov.getY());
                tab.wv.dispatchTouchEvent(m);
                m.recycle();
            }
            if (ac == MotionEvent.ACTION_UP && !fwd && tapOk) { setShown(!shown); if (shown) sched(); }
            return true;
        });
    }

    void start(MainActivity.Tab t, ArrayList<String> c, String ti, double pos, double dur,
               double dpr, double l, double tp, double w, double hh) {
        stop(false);
        tab = t;
        cands = c;
        ci = 0;
        expDur = dur;
        title = ti == null ? "" : ti;
        pos0 = (long) (pos * 1000);
        ua = t.wv.getSettings().getUserAgentString();
        if (ua == null) ua = "";
        page = t.wv.getUrl() == null ? "" : t.wv.getUrl();
        wentFull = false;
        build();
        active = true;
        move(dpr, l, tp, w, hh, true);
        load();
        h.post(upd);
        sched();
    }

    void move(double dpr, double l, double t, double w, double hh, boolean vis) {
        if (ov == null) return;
        int W = (int) Math.round(w * dpr), H = (int) Math.round(hh * dpr);
        if (W < 40 || H < 30) { ov.setVisibility(View.GONE); return; }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) ov.getLayoutParams();
        if (lp.width != W || lp.height != H) { lp.width = W; lp.height = H; ov.setLayoutParams(lp); }
        ov.setX((float) (l * dpr));
        ov.setY((float) (t * dpr));
        ov.setVisibility(vis ? View.VISIBLE : View.GONE);
    }

    void load() {
        advancing = false;
        if (player != null) { player.release(); player = null; }
        if (ci >= cands.size()) { fail(); return; }
        final String url = cands.get(ci);
        HashMap<String, String> hd = new HashMap<>();
        if (!page.isEmpty()) {
            hd.put("Referer", page);
            try { Uri pu = Uri.parse(page); hd.put("Origin", pu.getScheme() + "://" + pu.getHost()); } catch (Exception e) { }
        }
        String ck = CookieManager.getInstance().getCookie(url);
        if (ck != null) hd.put("Cookie", ck);
        DefaultHttpDataSource.Factory hf = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true).setDefaultRequestProperties(hd);
        if (!ua.isEmpty()) hf.setUserAgent(ua);
        player = new ExoPlayer.Builder(a).setMediaSourceFactory(new DefaultMediaSourceFactory(hf)).build();
        pv.setPlayer(player);
        player.setMediaItem(new MediaItem.Builder().setUri(Uri.parse(url))
                .setMimeType(url.toLowerCase().contains(".m3u8") ? MimeTypes.APPLICATION_M3U8 : null).build());
        verified = false;
        spin.setVisibility(View.VISIBLE);
        final ExoPlayer me = player;
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int s) {
                if (me != player) return;
                spin.setVisibility(s == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                if (s == Player.STATE_READY && !verified) { verified = true; check(); }
                refreshPlay();
            }
            @Override public void onIsPlayingChanged(boolean p) { if (me == player) { refreshPlay(); if (p) sched(); } }
            @Override public void onPlayerError(PlaybackException e) { if (me == player) next(); }
        });
        player.prepare();
        player.setPlayWhenReady(true);
    }

    void check() {
        long d = player.getDuration();
        if (expDur > 0 && d > 0 && d != C.TIME_UNSET) {
            double diff = Math.abs(d / 1000.0 - expDur);
            if (diff > Math.max(3, expDur * 0.03)) { next(); return; }
        }
        if (pos0 > 1500) player.seekTo(pos0);
        pos0 = 0;
    }

    void next() {
        if (advancing) return;
        advancing = true;
        h.post(() -> { if (!active) return; ci++; load(); });
    }

    void fail() {
        Toast.makeText(a, "Ye video app ke player me nahi chal paya, site ka player chalega", Toast.LENGTH_LONG).show();
        stop(true);
    }

    void refreshPlay() {
        if (player == null || playBtn == null) return;
        int st = player.getPlaybackState();
        boolean pl = player.getPlayWhenReady() && st != Player.STATE_ENDED && st != Player.STATE_IDLE;
        playBtn.setImageResource(pl ? R.drawable.ic_m_pause : R.drawable.ic_m_play);
    }

    void togglePlay() {
        if (player == null) return;
        if (player.getPlaybackState() == Player.STATE_ENDED) { player.seekTo(0); player.play(); }
        else if (player.getPlayWhenReady()) player.pause();
        else player.play();
    }

    void update() {
        if (player == null || ov == null) return;
        long d = player.getDuration();
        if (d == C.TIME_UNSET || d < 0) d = 0;
        long p = player.getCurrentPosition();
        if (!dragging) { curT.setText(fmt(p)); seek.setProgress(d > 0 ? (int) (p * 10000 / d) : 0); }
        durT.setText(fmt(d));
        seek.setSecondaryProgress(d > 0 ? (int) (player.getBufferedPosition() * 10000 / d) : 0);
    }

    void download() {
        if (ci >= cands.size()) return;
        String u = cands.get(ci);
        FastDownload.show(a, u, a.nameFor(u, title), ua);
    }

    void expand() {
        if (player == null || ci >= cands.size()) return;
        wentFull = true;
        wasPlaying = false;
        long p = player.getCurrentPosition();
        player.pause();
        Intent i = new Intent(a, PlayerActivity.class);
        i.putExtra("url", cands.get(ci));
        i.putExtra("name", title.isEmpty() ? "Video" : title);
        i.putExtra("pos", p / 1000.0);
        i.putExtra("ua", ua);
        i.putExtra("page", page);
        i.putExtra("land", true);
        a.startActivity(i);
    }

    void pause() {
        if (player != null) { wasPlaying = player.getPlayWhenReady(); player.pause(); }
    }

    void resume() {
        if (player == null || ci >= cands.size()) return;
        if (wentFull) {
            wentFull = false;
            long sp = a.getSharedPreferences("mt", 0).getLong("pp_" + cands.get(ci).hashCode(), -1);
            if (sp >= 0 && Math.abs(sp - player.getCurrentPosition()) > 1500) player.seekTo(sp);
        } else if (wasPlaying) player.play();
    }

    void stop(boolean releaseJs) {
        h.removeCallbacksAndMessages(null);
        active = false;
        if (player != null) { try { player.release(); } catch (Exception e) { } player = null; }
        if (ov != null) { try { host.removeView(ov); } catch (Exception e) { } ov = null; }
        MainActivity.Tab t = tab;
        tab = null;
        if (releaseJs && t != null && t.wv != null) {
            try { t.wv.evaluateJavascript("window.__mtrel&&window.__mtrel()", null); } catch (Exception e) { }
        }
    }
}
