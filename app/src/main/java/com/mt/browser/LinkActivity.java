package com.mt.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent in = getIntent();
        String u = null;
        if (in != null) {
            if (Intent.ACTION_VIEW.equals(in.getAction()) && in.getData() != null) u = in.getData().toString();
            else if (Intent.ACTION_SEND.equals(in.getAction())) {
                CharSequence t = in.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (t != null) {
                    Matcher m = Pattern.compile("https?://\\S+").matcher(t);
                    u = m.find() ? m.group() : t.toString().trim();
                }
            }
        }
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (u != null) i.putExtra("open_url", u);
        startActivity(i);
        finish();
    }
}
