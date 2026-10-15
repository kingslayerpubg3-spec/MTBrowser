package com.mt.browser;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class TabSwitcher {
    static ArrayList<String[]> closed = new ArrayList<>();
    static boolean incogMode;

    static int dp(MainActivity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    static void capture(MainActivity a, MainActivity.Tab t) {
        try {
            if (t == null || t.wv == null || !a.tabs.contains(t)) return;
            int w = t.wv.getWidth(), h = t.wv.getHeight();
            if (w <= 0 || h <= 0) return;
            Bitmap b = Bitmap.createBitmap(w / 3, h / 3, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(b);
            c.scale(0.3333f, 0.3333f);
            t.wv.draw(c);
            t.thumb = b;
        } catch (Throwable e) { }
    }

    static void show(MainActivity a) {
        capture(a, a.cur);
        incogMode = a.cur != null && a.cur.incog;
        Dialog d = new Dialog(a, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        build(a, d);
        d.getWindow().setWindowAnimations(android.R.style.Animation_Dialog);
        d.show();
    }

    static void build(final MainActivity a, final Dialog d) {
        final int bg = incogMode ? 0xFF1E1636 : (UiTheme.light(a) ? 0xFF0A1530 : UiTheme.bg2(a));
        final int accent = incogMode ? 0xFF9A86FF : UiTheme.acc(a);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(bg));
        DisplayMetrics dm = a.getResources().getDisplayMetrics();
        final int cardW = (int) (dm.widthPixels * 0.68f);
        final int cardH = (int) (dm.heightPixels * 0.50f);
        final int m = dp(a, 8);
        final ArrayList<MainActivity.Tab> items = new ArrayList<>();
        for (MainActivity.Tab t : a.tabs) if (t.incog == incogMode) items.add(t);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(0, dp(a, 30), 0, 0);

        TextView h1 = new TextView(a);
        h1.setText(incogMode ? "Incognito mode on" : "Tabs");
        h1.setTextColor(Color.WHITE);
        h1.setTextSize(22);
        h1.setGravity(Gravity.CENTER);
        root.addView(h1);
        TextView h2 = new TextView(a);
        h2.setText(incogMode ? "History save nahi hogi" : items.size() + " tabs  •  upar swipe = band");
        h2.setTextColor(0xFF9AA4BC);
        h2.setTextSize(13);
        h2.setGravity(Gravity.CENTER);
        h2.setPadding(dp(a, 24), dp(a, 6), dp(a, 24), dp(a, 16));
        root.addView(h2);

        if (items.isEmpty()) {
            TextView e = new TextView(a);
            e.setText("Koi tab nahi hai. + dabao.");
            e.setTextColor(0xFF9AA4BC);
            e.setGravity(Gravity.CENTER);
            root.addView(e, new LinearLayout.LayoutParams(-1, 0, 1f));
        } else {
            addPager(a, d, root, items, cardW, cardH, m, accent, dm);
        }
        addBar(a, d, root, accent);
        d.setContentView(root);
        d.getWindow().setLayout(-1, -1);
    }

    static void addPager(final MainActivity a, final Dialog d, LinearLayout root,
                         final ArrayList<MainActivity.Tab> items, final int cardW, final int cardH,
                         final int m, final int accent, DisplayMetrics dm) {
        final RecyclerView rv = new RecyclerView(a);
        final LinearLayoutManager lm = new LinearLayoutManager(a, LinearLayoutManager.HORIZONTAL, false);
        rv.setLayoutManager(lm);
        rv.setClipToPadding(false);
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        int side = (dm.widthPixels - cardW) / 2 - m;
        rv.setPadding(side, 0, side, 0);
        final PagerSnapHelper snap = new PagerSnapHelper();
        snap.attachToRecyclerView(rv);
        final TextView pos = new TextView(a);
        pos.setTextColor(0xFF9AA4BC);
        pos.setGravity(Gravity.CENTER);
        pos.setPadding(0, dp(a, 12), 0, dp(a, 12));
        final RecyclerView.Adapter[] adp = new RecyclerView.Adapter[1];

        final java.util.function.Consumer<MainActivity.Tab> remove = t -> {
            int p = items.indexOf(t);
            if (p < 0) return;
            boolean wasCur = t == a.cur;
            String u = t.wv.getUrl();
            if (u != null && !u.startsWith("file:")) closed.add(new String[]{u, t.incog ? "1" : "0"});
            items.remove(p);
            adp[0].notifyItemRemoved(p);
            a.closeTab(t);
            if (items.isEmpty()) { build(a, d); return; }
            rv.postDelayed(() -> {
                if (wasCur) adp[0].notifyItemRangeChanged(0, items.size());
                View sv = snap.findSnapView(lm);
                if (sv != null) pos.setText((lm.getPosition(sv) + 1) + " / " + items.size());
                scaleCards(rv);
            }, 280);
        };

        adp[0] = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public int getItemCount() { return items.size(); }

            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int vt) {
                LinearLayout col = new LinearLayout(a);
                col.setOrientation(LinearLayout.VERTICAL);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(cardW, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(m, 0, m, 0);
                col.setLayoutParams(lp);
                return new RecyclerView.ViewHolder(col) { };
            }

            @Override public void onBindViewHolder(RecyclerView.ViewHolder hd, int i) {
                final MainActivity.Tab t = items.get(i);
                final LinearLayout col = (LinearLayout) hd.itemView;
                col.removeAllViews();
                col.setTranslationY(0f);
                col.setAlpha(1f);
                TextView ti = new TextView(a);
                ti.setText(t.title);
                ti.setSingleLine(true);
                ti.setEllipsize(TextUtils.TruncateAt.END);
                ti.setTextColor(t == a.cur ? accent : 0xFF9AA4BC);
                ti.setTextSize(14);
                ti.setPadding(dp(a, 4), 0, dp(a, 4), dp(a, 8));
                col.addView(ti);

                FrameLayout card = new FrameLayout(a);
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(0xFF111A33);
                gd.setCornerRadius(dp(a, 14));
                if (t == a.cur) gd.setStroke(dp(a, 2), accent);
                card.setBackground(gd);
                card.setClipToOutline(true);

                ImageView iv = new ImageView(a);
                if (t.thumb != null) {
                    iv.setScaleType(ImageView.ScaleType.MATRIX);
                    iv.setImageBitmap(t.thumb);
                    float s = Math.max((float) cardW / t.thumb.getWidth(), (float) cardH / t.thumb.getHeight());
                    Matrix mx = new Matrix();
                    mx.setScale(s, s);
                    iv.setImageMatrix(mx);
                } else {
                    iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    iv.setPadding(dp(a, 50), dp(a, 50), dp(a, 50), dp(a, 50));
                    iv.setImageResource(R.drawable.ic_logo_full);
                }
                card.addView(iv, new FrameLayout.LayoutParams(-1, -1));

                TextView x = new TextView(a);
                x.setText("✕");
                x.setTextColor(Color.WHITE);
                x.setTextSize(20);
                x.setGravity(Gravity.CENTER);
                x.setBackgroundColor(0x99000000);
                card.addView(x, new FrameLayout.LayoutParams(-1, dp(a, 46), Gravity.BOTTOM));
                x.setOnClickListener(v -> col.animate().translationY(-cardH / 2f).alpha(0f).setDuration(200)
                        .withEndAction(() -> remove.accept(t)).start());
                card.setOnClickListener(v -> { a.switchTo(t); d.dismiss(); });
                col.addView(card, new LinearLayout.LayoutParams(-1, cardH));
            }
        };
        rv.setAdapter(adp[0]);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP) {
            @Override public boolean onMove(RecyclerView r, RecyclerView.ViewHolder v, RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(RecyclerView.ViewHolder v, int dir) {
                int p = v.getAdapterPosition();
                if (p >= 0 && p < items.size()) remove.accept(items.get(p));
            }
            @Override public float getSwipeThreshold(RecyclerView.ViewHolder v) { return 0.3f; }
            @Override public float getSwipeEscapeVelocity(float def) { return def * 0.5f; }
            @Override public void onChildDraw(Canvas c, RecyclerView r, RecyclerView.ViewHolder v, float dX, float dY, int st, boolean act) {
                super.onChildDraw(c, r, v, dX, dY, st, act);
                if (st == ItemTouchHelper.ACTION_STATE_SWIPE)
                    v.itemView.setAlpha(Math.max(0.1f, 1f - Math.abs(dY) / Math.max(1f, v.itemView.getHeight())));
            }
            @Override public void clearView(RecyclerView r, RecyclerView.ViewHolder v) {
                super.clearView(r, v);
                scaleCards(r);
            }
        }).attachToRecyclerView(rv);

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView r, int dx, int dy) { scaleCards(r); }
            @Override public void onScrollStateChanged(RecyclerView r, int s) {
                if (s == RecyclerView.SCROLL_STATE_IDLE) {
                    View v = snap.findSnapView(lm);
                    if (v != null) pos.setText((lm.getPosition(v) + 1) + " / " + items.size());
                }
            }
        });
        int ci = Math.max(0, items.indexOf(a.cur));
        lm.scrollToPositionWithOffset(ci, 0);
        pos.setText((ci + 1) + " / " + items.size());
        rv.post(() -> scaleCards(rv));
        root.addView(rv, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(pos);
    }

    static void addBar(final MainActivity a, final Dialog d, LinearLayout root, int accent) {
        LinearLayout bar = new LinearLayout(a);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(0, dp(a, 6), 0, dp(a, 28));

        ImageView gh = new ImageView(a);
        gh.setImageResource(R.drawable.ic_ghost);
        gh.setPadding(dp(a, 9), dp(a, 9), dp(a, 9), dp(a, 9));
        GradientDrawable gb = new GradientDrawable();
        gb.setShape(GradientDrawable.OVAL);
        gb.setColor(incogMode ? 0xFF6C4DE0 : 0x26FFFFFF);
        gh.setBackground(gb);
        gh.setColorFilter(0xFFFFFFFF);
        gh.setAlpha(incogMode ? 1f : 0.7f);
        gh.setOnClickListener(v -> { incogMode = !incogMode; build(a, d); });

        ImageView add = new ImageView(a);
        add.setImageResource(R.drawable.ic_m_add);
        add.setColorFilter(Color.WHITE);
        add.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));
        GradientDrawable ab = new GradientDrawable();
        ab.setShape(GradientDrawable.OVAL);
        ab.setColor(accent);
        add.setBackground(ab);
        add.setOnClickListener(v -> { d.dismiss(); a.newTab(incogMode); });

        ImageView un = new ImageView(a);
        un.setImageResource(R.drawable.ic_m_undo);
        un.setColorFilter(Color.WHITE);
        un.setPadding(dp(a, 12), dp(a, 12), dp(a, 12), dp(a, 12));
        un.setAlpha(closed.isEmpty() ? 0.35f : 1f);
        un.setOnClickListener(v -> {
            if (closed.isEmpty()) return;
            String[] e = closed.remove(closed.size() - 1);
            d.dismiss();
            a.newTab("1".equals(e[1]));
            a.cur.wv.loadUrl(e[0]);
        });

        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(dp(a, 38), dp(a, 38));
        l1.setMargins(dp(a, 36), 0, dp(a, 36), 0);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(dp(a, 64), dp(a, 64));
        LinearLayout.LayoutParams l3 = new LinearLayout.LayoutParams(dp(a, 52), dp(a, 52));
        l3.setMargins(dp(a, 36), 0, dp(a, 36), 0);
        bar.addView(gh, l1);
        bar.addView(add, l2);
        bar.addView(un, l3);
        root.addView(bar);
    }

    static void scaleCards(RecyclerView r) {
        float mid = r.getWidth() / 2f;
        for (int i = 0; i < r.getChildCount(); i++) {
            View c = r.getChildAt(i);
            float cc = (c.getLeft() + c.getRight()) / 2f;
            float dist = Math.min(1f, Math.abs(cc - mid) / Math.max(1f, r.getWidth() * 0.6f));
            float s = 1f - 0.14f * dist;
            c.setScaleX(s);
            c.setScaleY(s);
            if (c.getTranslationY() == 0f) c.setAlpha(1f - 0.25f * dist);
        }
    }
}
