package com.mt.browser;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity {
    static final String[] RN = {"Fit", "Fill", "Zoom"};
    static final int[] MODES = {AspectRatioFrameLayout.RESIZE_MODE_FIT, AspectRatioFrameLayout.RESIZE_MODE_FILL, AspectRatioFrameLayout.RESIZE_MODE_ZOOM};

    ExoPlayer player;
    PlayerView pv;
    FrameLayout root, ctrl;
    LinearLayout top, bottom, netBox;
    TextView title, clock, speedTv, netTv, curT, durT, osd;
    ImageView playBtn, lockBtn, backBtn, moreBtn, pipBtn, dlBtn, rotBtn, battIv;
    SeekBar seek;
    ProgressBar spin;
    String url, name, ua, page;
    boolean want720 = true;
    TextView qTv;
    java.util.ArrayList<String> alts = new java.util.ArrayList<>();
    boolean locked, shown = true, dragging, landscape = false, loop, noPip;
    int resize = 0, gMode;
    long startPos, lastRx = -1, lastTs, gPos, gTarget;
    float gBright, gVol;
    AudioManager am;
    GestureDetector gd;
    final Handler h = new Handler(Looper.getMainLooper());
    final Runnable updR = new Runnable() { public void run() { update(); h.postDelayed(this, 250); } };
    final Runnable hideR = new Runnable() { public void run() { if (!dragging) setCtrlVisible(false); } };
    final Runnable osdHide = new Runnable() { public void run() { osd.setVisibility(View.GONE); } };

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    ImageView icon(int res, int pad) {
        ImageView v = new ImageView(this);
        v.setImageResource(res);
        v.setPadding(dp(pad), dp(pad), dp(pad), dp(pad));
        return v;
    }

    TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setShadowLayer(4, 0, 1, 0xAA000000);
        return t;
    }

    static String fmt(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000, hh = s / 3600, m = (s % 3600) / 60, ss = s % 60;
        return hh > 0 ? String.format(Locale.US, "%d:%02d:%02d", hh, m, ss) : String.format(Locale.US, "%d:%02d", m, ss);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28)
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        Intent in = getIntent();
        url = in.getStringExtra("url");
        name = in.getStringExtra("name");
        ua = in.getStringExtra("ua");
        page = in.getStringExtra("page");
        String[] al0 = in.getStringArrayExtra("alts");
        if (al0 != null) for (String s0 : al0) alts.add(s0);
        startPos = (long) (in.getDoubleExtra("pos", 0) * 1000);
        if (name == null || name.isEmpty()) name = "Video";
        if (in.getBooleanExtra("land", false)) { landscape = true; setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE); }
        if (url == null) { finish(); return; }
        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        buildUi();
        initPlayer();
        immersive();
        h.post(updR);
    }

    void initPlayer() {
        HashMap<String, String> hd = new HashMap<>();
        if (page != null && !page.isEmpty()) { hd.put("Referer", page); try { Uri pu = Uri.parse(page); hd.put("Origin", pu.getScheme() + "://" + pu.getHost()); } catch (Exception e) { } }
        String ck = CookieManager.getInstance().getCookie(url);
        if (ck != null) hd.put("Cookie", ck);
        DefaultHttpDataSource.Factory hf = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(hd);
        if (ua != null && !ua.isEmpty()) hf.setUserAgent(ua);
        player = new ExoPlayer.Builder(this, new androidx.media3.exoplayer.DefaultRenderersFactory(this).setEnableDecoderFallback(true)).setLoadControl(new androidx.media3.exoplayer.DefaultLoadControl.Builder().setBufferDurationsMs(25000, 80000, 1500, 3000).build()).setMediaSourceFactory(new DefaultMediaSourceFactory(hf)).build(); player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC);
        pv.setPlayer(player);
        if (startPos == 0) startPos = getSharedPreferences("mt", 0).getLong("pp_" + url.hashCode(), 0);
        player.setMediaItem(new MediaItem.Builder().setUri(Uri.parse(url)).setMimeType(url.toLowerCase().contains(".m3u8") ? MimeTypes.APPLICATION_M3U8 : null).build());
        player.prepare();
        if (startPos > 0) player.seekTo(startPos);
        player.setPlayWhenReady(true);
        smartDefault();
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int s) {
                spin.setVisibility(s == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                refreshPlay();
            }
            @Override public void onIsPlayingChanged(boolean p) { refreshPlay(); if (p) scheduleHide(); }
            @Override public void onPlayerError(PlaybackException e) { onErr(e); }
        });
    }

    void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        pv = new PlayerView(this);
        pv.setUseController(false);
        pv.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        root.addView(pv, new FrameLayout.LayoutParams(-1, -1));
        ctrl = new FrameLayout(this);
        root.addView(ctrl, new FrameLayout.LayoutParams(-1, -1));

        top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(10), dp(8), dp(10), dp(26));
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xAA000000, 0x00000000}));
        backBtn = icon(R.drawable.ic_nav_back, 12);
        top.addView(backBtn, new LinearLayout.LayoutParams(dp(46), dp(46)));
        title = text(name, 16, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMargins(dp(8), 0, dp(12), 0);
        top.addView(title, tp);
        battIv = icon(R.drawable.ic_p_battery, 1);
        top.addView(battIv, new LinearLayout.LayoutParams(dp(28), dp(28)));
        clock = text("", 14, false);
        clock.setPadding(dp(6), 0, dp(10), 0);
        top.addView(clock, new LinearLayout.LayoutParams(-2, -2));
        moreBtn = icon(R.drawable.ic_p_more, 11);
        top.addView(moreBtn, new LinearLayout.LayoutParams(dp(46), dp(46)));
        ctrl.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        lockBtn = icon(R.drawable.ic_p_unlock, 12);
        FrameLayout.LayoutParams lk = new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER_VERTICAL | Gravity.START);
        lk.setMargins(dp(22), 0, 0, 0);
        ctrl.addView(lockBtn, lk);

        netBox = new LinearLayout(this);
        netBox.setOrientation(LinearLayout.VERTICAL);
        netBox.setGravity(Gravity.CENTER_HORIZONTAL);
        netBox.addView(icon(R.drawable.ic_p_net, 4), new LinearLayout.LayoutParams(dp(44), dp(44)));
        netTv = text("0 Kb/s", 13, false);
        netBox.addView(netTv, new LinearLayout.LayoutParams(-2, -2));
        FrameLayout.LayoutParams nb = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_VERTICAL | Gravity.END);
        nb.setMargins(0, 0, dp(22), 0);
        ctrl.addView(netBox, nb);

        playBtn = icon(R.drawable.ic_m_play, 12);
        ctrl.addView(playBtn, new FrameLayout.LayoutParams(dp(80), dp(80), Gravity.CENTER));
        spin = new ProgressBar(this);
        spin.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        spin.setVisibility(View.GONE);
        root.addView(spin, new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER));

        bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(dp(18), dp(30), dp(18), dp(10));
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xAA000000, 0x00000000}));
        LinearLayout r1 = new LinearLayout(this);
        r1.setGravity(Gravity.CENTER_VERTICAL);
        curT = text("0:00", 14, false);
        r1.addView(curT, new LinearLayout.LayoutParams(-2, -2));
        seek = new SeekBar(this);
        seek.setMax(10000);
        seek.setProgressDrawable(getDrawable(R.drawable.p_seek));
        seek.setThumb(getDrawable(R.drawable.p_thumb));
        seek.setSplitTrack(false);
        seek.setPadding(dp(14), 0, dp(14), 0);
        r1.addView(seek, new LinearLayout.LayoutParams(0, dp(30), 1f));
        durT = text("0:00", 14, false);
        r1.addView(durT, new LinearLayout.LayoutParams(-2, -2));
        bottom.addView(r1, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout r2 = new LinearLayout(this);
        r2.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        qTv = text("Auto", 15, true); qTv.setPadding(dp(12), dp(10), dp(12), dp(10)); r2.addView(qTv, new LinearLayout.LayoutParams(-2, -2));
        speedTv = text("1.0X", 16, true);
        speedTv.setPadding(dp(14), dp(10), dp(14), dp(10));
        r2.addView(speedTv, new LinearLayout.LayoutParams(-2, -2));
        pipBtn = icon(R.drawable.ic_m_pip, 10);
        r2.addView(pipBtn, new LinearLayout.LayoutParams(dp(46), dp(46)));
        dlBtn = icon(R.drawable.ic_dl_line, 10);
        r2.addView(dlBtn, new LinearLayout.LayoutParams(dp(46), dp(46)));
        rotBtn = icon(R.drawable.ic_p_rotate, 10);
        r2.addView(rotBtn, new LinearLayout.LayoutParams(dp(46), dp(46)));
        bottom.addView(r2, new LinearLayout.LayoutParams(-1, -2));
        ctrl.addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        osd = text("", 18, true);
        osd.setGravity(Gravity.CENTER);
        osd.setPadding(dp(22), dp(10), dp(22), dp(10));
        GradientDrawable og = new GradientDrawable();
        og.setColor(0x99000000);
        og.setCornerRadius(dp(22));
        osd.setBackground(og);
        osd.setVisibility(View.GONE);
        FrameLayout.LayoutParams ol = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        ol.setMargins(0, dp(70), 0, 0);
        root.addView(osd, ol);
        setContentView(root);
        wire();
    }

    void wire() {
        backBtn.setOnClickListener(v -> finish());
        playBtn.setOnClickListener(v -> { togglePlay(); scheduleHide(); });
        lockBtn.setOnClickListener(v -> toggleLock());
        moreBtn.setOnClickListener(v -> showMore());
        speedTv.setOnClickListener(v -> pickSpeed());
        qTv.setOnClickListener(v -> quality());
        pipBtn.setOnClickListener(v -> enterPip());
        dlBtn.setOnClickListener(v -> quality());
        rotBtn.setOnClickListener(v -> rotate());
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                long d = player.getDuration();
                if (u && d > 0) curT.setText(fmt(d * p / 10000));
            }
            public void onStartTrackingTouch(SeekBar s) { dragging = true; h.removeCallbacks(hideR); }
            public void onStopTrackingTouch(SeekBar s) {
                long d = player.getDuration();
                if (d > 0) player.seekTo(d * s.getProgress() / 10000);
                dragging = false;
                scheduleHide();
            }
        });
        gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { gMode = 0; return true; }
            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                if (shown) setCtrlVisible(false); else { setCtrlVisible(true); scheduleHide(); }
                return true;
            }
            @Override public boolean onDoubleTap(MotionEvent e) {
                if (locked) return true;
                float x = e.getX(), w = root.getWidth();
                if (x < w / 3f) seekBy(-10000); else if (x > w * 2f / 3f) seekBy(10000); else togglePlay();
                return true;
            }
            @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (locked || e1 == null) return false;
                scroll(e1, e2);
                return true;
            }
        });
        gd.setIsLongpressEnabled(false);
        root.setOnTouchListener((v, e) -> {
            gd.onTouchEvent(e);
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) endGesture();
            return true;
        });
    }

    void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    void setCtrlVisibleOld(boolean v) {
        shown = v;
        if (v) ctrl.setVisibility(View.VISIBLE);
        ctrl.animate().alpha(v ? 1f : 0f).setDuration(180).withEndAction(() -> { if (!shown) ctrl.setVisibility(View.GONE); }).start();
    }

    void scheduleHide() {
        h.removeCallbacks(hideR);
        if (player != null && player.isPlaying()) h.postDelayed(hideR, 3500);
    }

    void osd(String s) {
        osd.setText(s);
        osd.setVisibility(View.VISIBLE); osd.setAlpha(0f); osd.animate().alpha(1f).setDuration(120).start();
        h.removeCallbacks(osdHide);
        h.postDelayed(osdHide, 900);
    }

    void refreshPlay() {
        if (player == null) return;
        int st = player.getPlaybackState();
        boolean pl = player.getPlayWhenReady() && st != Player.STATE_ENDED && st != Player.STATE_IDLE;
        playBtn.setImageResource(pl ? R.drawable.ic_m_pause : R.drawable.ic_m_play);
        playBtn.setAlpha(st == Player.STATE_BUFFERING ? 0f : 1f);
    }

    void togglePlay() {
        if (player.getPlaybackState() == Player.STATE_ENDED) { player.seekTo(0); player.play(); }
        else if (player.getPlayWhenReady()) player.pause();
        else player.play();
    }

    void seekBy(long ms) {
        long p = Math.max(0, player.getCurrentPosition() + ms), d = player.getDuration();
        if (d > 0) p = Math.min(p, d);
        player.seekTo(p);
        osd((ms > 0 ? "+" : "") + (ms / 1000) + "s");
    }

    void toggleLock() {
        locked = !locked;
        lockBtn.setImageResource(locked ? R.drawable.ic_p_lock : R.drawable.ic_p_unlock);
        int o = locked ? View.GONE : View.VISIBLE;
        top.setVisibility(o);
        bottom.setVisibility(o);
        playBtn.setVisibility(o);
        netBox.setVisibility(o);
        osd(locked ? "Screen locked" : "Unlocked");
        scheduleHide();
    }

    void scroll(MotionEvent e1, MotionEvent e2) {
        if (gMode == 0) {
            if (Math.abs(e2.getX() - e1.getX()) > Math.abs(e2.getY() - e1.getY())) { gMode = 1; gPos = player.getCurrentPosition(); gTarget = gPos; }
            else if (e1.getX() < root.getWidth() / 2f) {
                gMode = 2;
                float b = getWindow().getAttributes().screenBrightness;
                gBright = b < 0 ? 0.5f : b;
            } else { gMode = 3; gVol = am.getStreamVolume(AudioManager.STREAM_MUSIC); }
        }
        if (gMode == 1) {
            long d = player.getDuration();
            if (d <= 0) return;
            gTarget = Math.max(0, Math.min(d, gPos + (long) ((e2.getX() - e1.getX()) / root.getWidth() * 90000)));
            osd(fmt(gTarget) + " / " + fmt(d));
        } else if (gMode == 2) {
            float nb = Math.max(0.02f, Math.min(1f, gBright + (e1.getY() - e2.getY()) / root.getHeight()));
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = nb;
            getWindow().setAttributes(lp);
            osd("Brightness " + (int) (nb * 100) + "%");
        } else {
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            float nv = Math.max(0f, Math.min((float) max, gVol + (e1.getY() - e2.getY()) / root.getHeight() * max * 1.2f));
            am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(nv), 0);
            osd("Volume " + Math.round(nv * 100f / max) + "%");
        }
    }

    void endGesture() {
        if (gMode == 1) player.seekTo(gTarget);
        gMode = 0;
    }

    void update() {
        if (player == null) return;
        long d = player.getDuration();
        if (d == C.TIME_UNSET || d < 0) d = 0;
        long p = player.getCurrentPosition();
        if (!dragging) {
            curT.setText(fmt(p));
            seek.setProgress(d > 0 ? (int) (p * 10000 / d) : 0);
        }
        durT.setText(fmt(d));
        { androidx.media3.common.Format vf = player.getVideoFormat(); if (vf != null && vf.height > 0) qTv.setText(vf.height + "p"); }
        seek.setSecondaryProgress(d > 0 ? (int) (player.getBufferedPosition() * 10000 / d) : 0);
        long now = System.currentTimeMillis();
        if (now - lastTs >= 1000) {
            long rx = TrafficStats.getUidRxBytes(android.os.Process.myUid());
            if (rx == TrafficStats.UNSUPPORTED) rx = TrafficStats.getTotalRxBytes();
            if (lastRx >= 0 && rx >= lastRx) {
                double bps = (rx - lastRx) * 1000.0 / (now - lastTs);
                netTv.setText(bps >= 1048576 ? String.format(Locale.US, "%.1f Mb/s", bps / 1048576) : String.format(Locale.US, "%.0f Kb/s", bps / 1024));
            }
            lastRx = rx;
            lastTs = now;
            clock.setText(new SimpleDateFormat("HH:mm", Locale.US).format(new Date()));
        }
    }

    void pickSpeed() {
        final float[] v = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
        new AlertDialog.Builder(this).setTitle("Playback speed")
            .setItems(new String[]{"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"}, (d, i) -> {
                player.setPlaybackParameters(new PlaybackParameters(v[i]));
                speedTv.setText((v[i] == (int) v[i] ? (int) v[i] + ".0" : String.valueOf(v[i])) + "X");
            }).show();
    }

    void download() {
        String l = url.toLowerCase();
        if (l.contains(".mpd")) { Toast.makeText(this, "Ye stream video hai, seedha download nahi hota", Toast.LENGTH_LONG).show(); return; }
        if (!url.startsWith("http")) { Toast.makeText(this, "Ye file pehle se phone me hai", Toast.LENGTH_SHORT).show(); return; }
        String nm = URLUtil.guessFileName(url, null, null), ln = nm.toLowerCase();
        if (ln.endsWith(".m3u8") || ln.endsWith(".bin") || !nm.contains(".") || ln.startsWith("download") || ln.startsWith("videoplayback"))
            nm = name.replaceAll("[\\\\/:*?\"<>|]", " ").trim() + ".mp4";
        FastDownload.show(this, url, nm, ua);
    }

    void showMore() {
        String[] it = {"Subtitle load karo", "Audio track", "Aspect ratio: " + RN[resize], "Loop: " + (loop ? "On" : "Off"), "Dusre player me kholo", "Link share karo"};
        new AlertDialog.Builder(this).setItems(it, (d, i) -> {
            switch (i) {
                case 0: pickSub(); break;
                case 1: audioTracks(); break;
                case 2: resize = (resize + 1) % 3; pv.setResizeMode(MODES[resize]); osd("Aspect: " + RN[resize]); break;
                case 3: loop = !loop; player.setRepeatMode(loop ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF); osd("Loop " + (loop ? "On" : "Off")); break;
                case 4: external(); break;
                case 5:
                    noPip = true;
                    Intent s = new Intent(Intent.ACTION_SEND);
                    s.setType("text/plain");
                    s.putExtra(Intent.EXTRA_TEXT, url);
                    startActivity(Intent.createChooser(s, "Share"));
                    break;
            }
        }).show();
    }

    void audioTracks() {
        final ArrayList<Tracks.Group> gs = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        for (Tracks.Group g : player.getCurrentTracks().getGroups()) {
            if (g.getType() != C.TRACK_TYPE_AUDIO) continue;
            Format f = g.getTrackFormat(0);
            String n = f.label != null ? f.label : (f.language != null ? f.language : "Track " + (gs.size() + 1));
            if (f.channelCount > 0) n += " • " + f.channelCount + "ch";
            if (!g.isSupported()) n += " (phone support nahi)";
            gs.add(g);
            names.add(n);
        }
        if (gs.isEmpty()) { Toast.makeText(this, "Koi audio track nahi mila", Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(this).setTitle("Audio track").setItems(names.toArray(new String[0]), (d, i) ->
            player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .setOverrideForType(new TrackSelectionOverride(gs.get(i).getMediaTrackGroup(), 0)).build())).show();
    }

    void pickSub() {
        noPip = true;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, 55);
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc != 55 || res != RESULT_OK || d == null || d.getData() == null) return;
        Uri u = d.getData();
        String nm = "";
        try (android.database.Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (ix >= 0) nm = c.getString(ix);
            }
        } catch (Exception e) { }
        String l = nm.toLowerCase();
        String mime = l.endsWith(".vtt") ? MimeTypes.TEXT_VTT : (l.endsWith(".ass") || l.endsWith(".ssa")) ? MimeTypes.TEXT_SSA : MimeTypes.APPLICATION_SUBRIP;
        MediaItem.SubtitleConfiguration sc = new MediaItem.SubtitleConfiguration.Builder(u)
                .setMimeType(mime).setLanguage("und").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build();
        long p = player.getCurrentPosition();
        player.setMediaItem(new MediaItem.Builder().setUri(Uri.parse(url)).setMimeType(url.toLowerCase().contains(".m3u8") ? MimeTypes.APPLICATION_M3U8 : null).setSubtitleConfigurations(Collections.singletonList(sc)).build(), p);
        player.prepare();
        player.play();
        osd("Subtitle load ho gaya");
    }

    void external() {
        noPip = true;
        player.pause();
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse(url), "video/*");
            startActivity(Intent.createChooser(i, "Play with"));
        } catch (Exception e) { Toast.makeText(this, "Koi player nahi mila", Toast.LENGTH_SHORT).show(); }
    }

    void onErr(PlaybackException e) {
        if (isFinishing()) return;
        new AlertDialog.Builder(this).setTitle("Video play nahi ho paya")
            .setMessage(e.getErrorCodeName() + "\nSite ye video app ke player me nahi chalne de rahi, ya format supported nahi hai.")
            .setPositiveButton("Dusre player me", (d, w) -> external())
            .setNegativeButton("Band karo", (d, w) -> finish()).show();
    }

    void rotate() {
        landscape = !landscape;
        setRequestedOrientation(landscape ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    void enterPip() {
        try {
            enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(16, 9)).build());
        } catch (Exception e) { Toast.makeText(this, "PiP is device me nahi chal raha", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!noPip && player != null && player.isPlaying()) enterPip();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean pip, Configuration c) {
        super.onPictureInPictureModeChanged(pip, c);
        if (pip) ctrl.setVisibility(View.GONE);
        else { ctrl.setAlpha(1f); ctrl.setVisibility(View.VISIBLE); shown = true; scheduleHide(); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        noPip = false;
        immersive();
    }

    @Override
    public void onWindowFocusChanged(boolean f) {
        super.onWindowFocusChanged(f);
        if (f) immersive();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player == null) return;
        long p = player.getCurrentPosition(), d = player.getDuration();
        if (d > 0 && p > d - 5000) p = 0;
        getSharedPreferences("mt", 0).edit().putLong("pp_" + url.hashCode(), p).apply();
        if (!isInPictureInPictureMode()) player.pause();
    }

    @Override
    public void onBackPressed() {
        if (locked) { osd("Pehle unlock karo"); setCtrlVisible(true); scheduleHide(); return; }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }

    void setCtrlVisible(boolean v) {
        shown = v;
        if (v) ctrl.setVisibility(View.VISIBLE);
        ctrl.animate().alpha(v ? 1f : 0f).setDuration(220).withEndAction(() -> { if (!shown) ctrl.setVisibility(View.GONE); }).start();
        top.animate().translationY(v ? 0f : -dp(14)).setDuration(220).start();
        bottom.animate().translationY(v ? 0f : dp(14)).setDuration(220).start();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    void quality() {
        if (player == null) return;
        if (!url.startsWith("http")) { Toast.makeText(this, "Ye file pehle se phone me hai", Toast.LENGTH_SHORT).show(); return; }
        if (url.toLowerCase().contains(".mpd")) { Toast.makeText(this, "Is stream ka download nahi hota", Toast.LENGTH_SHORT).show(); return; }
        long du = player.getDuration();
        Qualities.show(this, url, alts, ua, page, du > 0 ? du / 1000.0 : 0, name, player, this::playQ);
    }

    void playQOld(Qualities.Q q) {
        if (player == null) return;
        if (q.auto) { Qualities.setHeight(player, 0); osd("Auto quality"); return; }
        if (q.hls) { Qualities.setHeight(player, q.height); osd("Quality: " + q.label); return; }
        if (q.url.equals(url)) return;
        long p = player.getCurrentPosition();
        url = q.url;
        player.setMediaItem(new MediaItem.Builder().setUri(Uri.parse(url))
                .setMimeType(url.toLowerCase().contains(".m3u8") ? MimeTypes.APPLICATION_M3U8 : null).build(), p);
        player.prepare();
        player.play();
        osd("Quality: " + q.label);
    }

    void playQ(Qualities.Q q) {
        if (player == null) return;
        want720 = false; String nu = q.auto ? q.master : q.url;
        if (nu == null || nu.isEmpty()) { if (q.auto) Qualities.setHeight(player, 0); return; }
        if (nu.equals(url)) { if (q.auto) Qualities.setHeight(player, 0); return; }
        long p = player.getCurrentPosition();
        url = nu;
        player.setMediaItem(new MediaItem.Builder().setUri(Uri.parse(url))
                .setMimeType(url.toLowerCase().contains(".m3u8") ? MimeTypes.APPLICATION_M3U8 : null).build(), p);
        player.prepare();
        player.play();
        osd(q.auto ? "Auto quality" : "Quality: " + q.label);
    }

    void smartDefault() {
        if (url == null || !url.startsWith("http")) return;
        final String u0 = url;
        player.addListener(new Player.Listener() {
            @Override public void onTracksChanged(Tracks t) {
                if (!want720 || player == null) return;
                int n = 0;
                for (Tracks.Group g : t.getGroups()) if (g.getType() == C.TRACK_TYPE_VIDEO) n += g.length;
                if (n > 1) { want720 = false; Qualities.setDefaultHeight(player, 720); }
            }
        });
        new Thread(() -> {
            final String[] r = Qualities.pickDefault(u0, alts, ua, page, 720);
            runOnUiThread(() -> {
                if (player == null || r == null || !url.equals(u0) || r[0].equals(u0)) return;
                long p = player.getCurrentPosition();
                url = r[0];
                want720 = true;
                player.setMediaItem(new MediaItem.Builder().setUri(Uri.parse(url))
                        .setMimeType(url.toLowerCase().contains(".m3u8") ? MimeTypes.APPLICATION_M3U8 : null).build(), p);
                player.prepare();
                player.play();
            });
        }).start();
    }
}
