package com.mt.browser;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;

public class LibraryActivity extends AppCompatActivity {
    String mode, key;
    ListView list;
    TextView empty;
    ArrayList<String> rows = new ArrayList<>();
    ArrayList<String> urls = new ArrayList<>();
    ArrayList<Long> ids = new ArrayList<>();
    ArrayList<Boolean> done = new ArrayList<>();
    ArrayAdapter<String> ad;
    boolean showDone = false;
    Handler h = new Handler(Looper.getMainLooper());
    Runnable tick = new Runnable() {
        public void run() { refresh(); h.postDelayed(this, 1000); }
    };

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    Button btn(String t) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextColor(Color.parseColor("#2E8BFF"));
        return b;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "bookmarks";
        key = mode.equals("history") ? "hist" : "bm";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#050B1E"));
        root.setPadding(0, dp(16), 0, 0);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setPadding(dp(16), dp(8), dp(8), dp(8));
        TextView title = new TextView(this);
        title.setText(mode.equals("downloads") ? "Downloads" : mode.equals("history") ? "History" : "Bookmarks");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (!mode.equals("downloads")) {
            Button clr = btn("Clear all");
            clr.setOnClickListener(v -> { Store.clear(this, key); refresh(); });
            top.addView(clr);
        }
        root.addView(top);

        if (mode.equals("downloads")) {
            LinearLayout f = new LinearLayout(this);
            f.setOrientation(LinearLayout.HORIZONTAL);
            Button a = btn("Downloading");
            Button c = btn("Completed");
            a.setOnClickListener(v -> { showDone = false; refresh(); });
            c.setOnClickListener(v -> { showDone = true; refresh(); });
            f.addView(a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            f.addView(c, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            root.addView(f);
        }

        TextView hint = new TextView(this);
        hint.setText("Tap = open  •  Long press = delete");
        hint.setTextColor(Color.parseColor("#8A94A8"));
        hint.setTextSize(12);
        hint.setPadding(dp(16), 0, dp(16), dp(8));
        root.addView(hint);

        empty = new TextView(this);
        empty.setText("Kuch nahi hai yahan");
        empty.setTextColor(Color.parseColor("#8A94A8"));
        empty.setPadding(dp(16), dp(24), dp(16), 0);
        root.addView(empty);

        list = new ListView(this);
        ad = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows) {
            @Override public View getView(int p, View cv, ViewGroup parent) {
                TextView t = (TextView) super.getView(p, cv, parent);
                t.setTextColor(Color.WHITE);
                t.setTextSize(14);
                t.setMaxLines(3);
                t.setPadding(dp(16), dp(12), dp(16), dp(12));
                return t;
            }
        };
        list.setAdapter(ad);
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        list.setOnItemClickListener((p, v, pos, rid) -> {
            if (mode.equals("downloads")) {
                if (!done.get(pos)) return;
                try {
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    Uri u = dm.getUriForDownloadedFile(ids.get(pos));
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(u, dm.getMimeTypeForDownloadedFile(ids.get(pos)));
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(this, "File open nahi ho saki", Toast.LENGTH_SHORT).show();
                }
            } else {
                Intent r = new Intent();
                r.putExtra("url", urls.get(pos));
                setResult(RESULT_OK, r);
                finish();
            }
        });

        list.setOnItemLongClickListener((p, v, pos, rid) -> {
            if (mode.equals("downloads")) {
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).remove(ids.get(pos));
            } else {
                Store.remove(this, key, pos);
            }
            refresh();
            return true;
        });

        if (mode.equals("downloads")) h.post(tick); else refresh();
    }

    String fmt(long b) {
        if (b <= 0) return "?";
        if (b < 1048576) return (b / 1024) + " KB";
        if (b < 1073741824L) return String.format(Locale.US, "%.1f MB", b / 1048576.0);
        return String.format(Locale.US, "%.2f GB", b / 1073741824.0);
    }

    void refresh() {
        rows.clear(); urls.clear(); ids.clear(); done.clear();
        if (mode.equals("downloads")) {
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Cursor c = dm.query(new DownloadManager.Query());
            if (c != null) {
                while (c.moveToNext()) {
                    long id = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                    String name = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
                    if (name == null || name.isEmpty()) name = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_URI));
                    int st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    long got = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    long tot = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    boolean fin = st == DownloadManager.STATUS_SUCCESSFUL;
                    boolean failed = st == DownloadManager.STATUS_FAILED;
                    if ((fin || failed) != showDone) continue;
                    String info;
                    if (fin) info = "Completed • " + fmt(tot);
                    else if (failed) info = "Failed";
                    else if (st == DownloadManager.STATUS_PAUSED) info = "Paused • " + fmt(got);
                    else {
                        int pc = tot > 0 ? (int) (got * 100 / tot) : 0;
                        info = "Downloading " + pc + "% • " + fmt(got) + " / " + fmt(tot);
                    }
                    rows.add(name + "\n" + info);
                    ids.add(id);
                    done.add(fin);
                    urls.add("");
                }
                c.close();
            }
        } else {
            JSONArray a = Store.get(this, key);
            try {
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    rows.add(o.getString("title") + "\n" + o.getString("url"));
                    urls.add(o.getString("url"));
                }
            } catch (Exception e) { }
        }
        ad.notifyDataSetChanged();
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(tick);
        super.onDestroy();
    }
}
