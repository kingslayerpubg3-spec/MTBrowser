package com.mt.browser;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.BassBoost;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Virtualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public class PlayerTools {
    static final int N = 18;
    static final int[] ORDER = {0, 1, 17, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 2, 3, 4, 15, 16};
    static final String[] LABEL = {"Night Mode", "Customise Items", "Subtitle", "Audio Track", "Aspect", "Loop", "Mute",
            "Sleep Timer", "A - B Repeat", "Mirror Mode", "Vertical Flip", "Audio Effect", "Speed", "Screenshot",
            "Background Play", "Other Player", "Share", "Shuffle"};
    static final int[] ICON = {R.drawable.ic_pl_night, R.drawable.ic_m_edit, R.drawable.ic_pl_cc, R.drawable.ic_pl_audio,
            R.drawable.ic_pl_aspect, R.drawable.ic_pl_loop, R.drawable.ic_pl_mute, R.drawable.ic_pl_sleep, R.drawable.ic_pl_ab,
            R.drawable.ic_pl_mirror, R.drawable.ic_pl_vflip, R.drawable.ic_pl_fx, R.drawable.ic_m_speed, R.drawable.ic_pl_shot,
            R.drawable.ic_pl_bg, R.drawable.ic_pl_open, R.drawable.ic_pl_share, R.drawable.ic_pl_shuffle};

    static boolean bg, muted, mirror, vflip, night;
    static long abA = -1, abB = -1;
    static int fxMode;
    static HorizontalScrollView panel;
    static View nightView;
    static PlayerActivity act;
    static Runnable abR, sleepR;
    static BassBoost bass;
    static LoudnessEnhancer loud;
    static Virtualizer virt;
    static final Handler h = new Handler(Looper.getMainLooper());
    static final Runnable autoR = new Runnable() { public void run() { if (act != null) hide(act); } };

    static int dp(PlayerActivity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    static void reset() {
        stopAb();
        abA = -1;
        abB = -1;
        if (sleepR != null) { h.removeCallbacks(sleepR); sleepR = null; }
        releaseFx();
        fxMode = 0;
        muted = false;
        mirror = false;
        vflip = false;
        night = false;
        bg = false;
        panel = null;
        nightView = null;
    }

    public static void release(PlayerActivity a) {
        h.removeCallbacks(autoR);
        if (act == a) { reset(); act = null; }
    }

    public static void toggle(PlayerActivity a) {
        if (act != a) { reset(); act = a; }
        if (panel != null) hide(a); else show(a, true);
    }

    static boolean hidden(PlayerActivity a, int id) {
        if (id == 1) return false;
        Set<String> s = a.getSharedPreferences("mt", 0).getStringSet("pl_hide", new HashSet<String>());
        return s.contains(String.valueOf(id));
    }

    static boolean active(int id) {
        switch (id) {
            case 0: return night;
            case 5: return act != null && act.loop;
            case 6: return muted;
            case 7: return sleepR != null;
            case 8: return abA >= 0;
            case 9: return mirror;
            case 10: return vflip;
            case 11: return fxMode != 0;
            case 14: return bg;
            case 17: return act != null && act.shuffle;
            default: return false;
        }
    }

    static void show(PlayerActivity a, boolean anim) {
        LinearLayout row = new LinearLayout(a);
        row.setPadding(dp(a, 8), 0, dp(a, 8), 0);
        for (int id : ORDER)
            if (!hidden(a, id)) row.addView(tileNew(a, id), new LinearLayout.LayoutParams(dp(a, 72), -2));
        panel = new HorizontalScrollView(a);
        panel.setHorizontalScrollBarEnabled(false);
        panel.setOverScrollMode(View.OVER_SCROLL_NEVER);
        panel.addView(row);
        panel.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x99000000, 0x00000000}));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        lp.topMargin = dp(a, 64);
        a.root.addView(panel, lp);
        if (anim) {
            panel.setAlpha(0f);
            panel.setTranslationY(-dp(a, 10));
            panel.animate().alpha(1f).translationY(0f).setDuration(200).start();
        }
        h.removeCallbacks(autoR);
        h.postDelayed(autoR, 8000);
    }

    static void hide(final PlayerActivity a) {
        h.removeCallbacks(autoR);
        final HorizontalScrollView p = panel;
        panel = null;
        if (p == null) return;
        p.animate().alpha(0f).setDuration(150).withEndAction(() -> a.root.removeView(p)).start();
    }

    static void refresh(PlayerActivity a) {
        if (panel == null) return;
        final int sx = panel.getScrollX();
        a.root.removeView(panel);
        panel = null;
        show(a, false);
        final HorizontalScrollView p = panel;
        p.post(() -> p.scrollTo(sx, 0));
    }

    static View tile(final PlayerActivity a, final int id) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        boolean on = active(id);
        ImageView iv = new ImageView(a);
        iv.setImageResource(ICON[id]);
        iv.setColorFilter(on ? UiTheme.acc(a) : Color.WHITE);
        int p = dp(a, 17);
        iv.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(on ? 0x66FFFFFF : 0x66333333);
        iv.setBackground(g);
        c.addView(iv, new LinearLayout.LayoutParams(dp(a, 58), dp(a, 58)));
        TextView t = new TextView(a);
        t.setText(LABEL[id]);
        t.setTextColor(Color.WHITE);
        t.setTextSize(11);
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setShadowLayer(4, 0, 1, 0xAA000000);
        t.setPadding(0, dp(a, 5), 0, 0);
        c.addView(t, new LinearLayout.LayoutParams(dp(a, 70), -2));
        c.setOnClickListener(v -> run(a, id));
        MenuSheet.press(c);
        return c;
    }

    static void run(PlayerActivity a, int id) {
        if (a.player == null) return;
        h.removeCallbacks(autoR);
        h.postDelayed(autoR, 8000);
        switch (id) {
            case 0: night = !night; applyNight(a); refresh(a); break;
            case 1: customise(a); break;
            case 2: hide(a); a.pickSub(); break;
            case 3: hide(a); a.audioTracks(); break;
            case 4: a.cycleAspect(); break;
            case 5: a.toggleLoop(); refresh(a); break;
            case 6: muted = !muted; a.player.setVolume(muted ? 0f : 1f); a.osd(muted ? "Mute on" : "Mute off"); refresh(a); break;
            case 7: hide(a); sleepDialog(a); break;
            case 8: abClick(a); refresh(a); break;
            case 9: mirror = !mirror; applyFlip(a); a.osd(mirror ? "Mirror on" : "Mirror off"); refresh(a); break;
            case 10: vflip = !vflip; applyFlip(a); a.osd(vflip ? "Vertical flip on" : "Vertical flip off"); refresh(a); break;
            case 11: hide(a); fxDialog(a); break;
            case 12: hide(a); a.pickSpeed(); break;
            case 13: screenshot(a); break;
            case 14: bg = !bg; a.osd(bg ? "Background play on: Home dabane par bhi awaaz chalegi" : "Background play off"); refresh(a); break;
            case 15: hide(a); a.external(); break;
            case 16: hide(a); a.shareUrl(); break;
            case 17: a.toggleShuffle(); refresh(a); break;
        }
    }

    static void applyNight(PlayerActivity a) {
        if (night) {
            if (nightView == null) {
                nightView = new View(a);
                nightView.setBackgroundColor(0x99000000);
                a.root.addView(nightView, 1, new FrameLayout.LayoutParams(-1, -1));
            }
        } else if (nightView != null) {
            a.root.removeView(nightView);
            nightView = null;
        }
        a.osd(night ? "Night mode on" : "Night mode off");
    }

    static void applyFlip(PlayerActivity a) {
        View s = a.pv.getVideoSurfaceView();
        if (s == null) return;
        s.setScaleX(mirror ? -1f : 1f);
        s.setScaleY(vflip ? -1f : 1f);
    }

    static void customise(final PlayerActivity a) {
        Set<String> hid = a.getSharedPreferences("mt", 0).getStringSet("pl_hide", new HashSet<String>());
        String[] it = new String[N];
        for (int i = 0; i < N; i++) it[i] = (hid.contains(String.valueOf(i)) ? "Off  •  " : "On  •  ") + LABEL[i];
        new AlertDialog.Builder(a).setTitle("Customise Items (tap = on ya off)").setItems(it, (d, i) -> {
            Set<String> s = new HashSet<>(a.getSharedPreferences("mt", 0).getStringSet("pl_hide", new HashSet<String>()));
            if (i == 1) a.osd("Customise Items hamesha dikhta hai");
            else {
                String k = String.valueOf(i);
                if (!s.remove(k)) s.add(k);
                a.getSharedPreferences("mt", 0).edit().putStringSet("pl_hide", s).apply();
            }
            refresh(a);
            customise(a);
        }).show();
    }

    static void sleepDialog(final PlayerActivity a) {
        final int[] m = {0, 5, 10, 15, 30, 45, 60};
        new AlertDialog.Builder(a).setTitle("Sleep Timer")
            .setItems(new String[]{"Off", "5 min", "10 min", "15 min", "30 min", "45 min", "60 min"}, (d, i) -> setSleep(a, m[i])).show();
    }

    static void setSleep(final PlayerActivity a, int min) {
        if (sleepR != null) { h.removeCallbacks(sleepR); sleepR = null; }
        if (min <= 0) { a.osd("Sleep timer off"); return; }
        sleepR = () -> {
            sleepR = null;
            try { if (a.player != null) a.player.pause(); } catch (Exception e) { }
            a.finish();
        };
        h.postDelayed(sleepR, min * 60000L);
        a.osd("Video " + min + " min baad band hoga");
    }

    static void abClick(PlayerActivity a) {
        long p = a.player.getCurrentPosition();
        if (abA < 0) {
            abA = p;
            a.osd("A set: " + PlayerActivity.fmt(p));
        } else if (abB < 0) {
            if (p <= abA + 500) { a.osd("B, A ke baad hona chahiye"); return; }
            abB = p;
            a.osd("A-B repeat on");
            startAb(a);
        } else {
            abA = -1;
            abB = -1;
            stopAb();
            a.osd("A-B repeat off");
        }
    }

    static void startAb(final PlayerActivity a) {
        stopAb();
        abR = new Runnable() {
            public void run() {
                if (a.player != null && abB > 0 && a.player.getCurrentPosition() >= abB) a.player.seekTo(abA);
                h.postDelayed(this, 200);
            }
        };
        h.postDelayed(abR, 200);
    }

    static void stopAb() {
        if (abR != null) { h.removeCallbacks(abR); abR = null; }
    }

    static void fxDialog(final PlayerActivity a) {
        new AlertDialog.Builder(a).setTitle("Audio Effect")
            .setItems(new String[]{"Normal", "Bass boost", "Loud (volume boost)", "Bass + Loud", "Surround (virtualizer)"}, (d, i) -> setFx(a, i)).show();
    }

    static void releaseFx() {
        try { if (bass != null) bass.release(); } catch (Throwable e) { }
        try { if (loud != null) loud.release(); } catch (Throwable e) { }
        try { if (virt != null) virt.release(); } catch (Throwable e) { }
        bass = null;
        loud = null;
        virt = null;
    }

    static void setFx(PlayerActivity a, int m) {
        releaseFx();
        fxMode = m;
        if (m == 0) { a.osd("Audio effect off"); return; }
        try {
            int sid = a.player.getAudioSessionId();
            if (m == 1 || m == 3) { bass = new BassBoost(0, sid); bass.setStrength((short) 800); bass.setEnabled(true); }
            if (m == 2 || m == 3) { loud = new LoudnessEnhancer(sid); loud.setTargetGain(1200); loud.setEnabled(true); }
            if (m == 4) { virt = new Virtualizer(0, sid); virt.setStrength((short) 800); virt.setEnabled(true); }
            a.osd("Audio effect on");
        } catch (Throwable e) {
            releaseFx();
            fxMode = 0;
            a.osd("Is phone par ye effect nahi chal raha");
        }
    }

    static void screenshot(PlayerActivity a) {
        try {
            View s = a.pv.getVideoSurfaceView();
            if (!(s instanceof TextureView)) { a.osd("Screenshot is mode me nahi ho sakta"); return; }
            Bitmap b = ((TextureView) s).getBitmap();
            if (b == null) { a.osd("Screenshot nahi mila"); return; }
            if (Build.VERSION.SDK_INT < 29) { a.osd("Screenshot ke liye Android 10 ya upar chahiye"); return; }
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, "MT_" + System.currentTimeMillis() + ".jpg");
            v.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MT Browser");
            Uri u = a.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            OutputStream os = a.getContentResolver().openOutputStream(u);
            b.compress(Bitmap.CompressFormat.JPEG, 92, os);
            os.close();
            a.osd("Screenshot save: Pictures/MT Browser");
        } catch (Throwable e) {
            a.osd("Screenshot fail ho gaya");
        }
    }

    static View tileNew(final PlayerActivity a, final int id) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        boolean on = active(id);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(on ? 0x88FFFFFF : 0x88444444);
        View face;
        if (id == 12) {
            TextView s = new TextView(a);
            s.setText(a.speedTv.getText().toString().replace(".0", ""));
            s.setTextColor(Color.WHITE);
            s.setTextSize(19);
            s.setTypeface(null, android.graphics.Typeface.BOLD);
            s.setGravity(Gravity.CENTER);
            face = s;
        } else {
            ImageView iv = new ImageView(a);
            iv.setImageResource(ICON[id]);
            iv.setColorFilter(on ? UiTheme.acc(a) : Color.WHITE);
            int p = dp(a, 17);
            iv.setPadding(p, p, p, p);
            face = iv;
        }
        face.setBackground(g);
        c.addView(face, new LinearLayout.LayoutParams(dp(a, 58), dp(a, 58)));
        TextView t = new TextView(a);
        t.setText(LABEL[id]);
        t.setTextColor(Color.WHITE);
        t.setTextSize(11);
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setShadowLayer(4, 0, 1, 0xAA000000);
        t.setPadding(0, dp(a, 5), 0, 0);
        c.addView(t, new LinearLayout.LayoutParams(dp(a, 70), -2));
        c.setOnClickListener(v -> run(a, id));
        MenuSheet.press(c);
        return c;
    }
}
