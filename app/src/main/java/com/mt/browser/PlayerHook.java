package com.mt.browser;

import android.content.Intent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import org.json.JSONObject;
import org.json.JSONTokener;

public class PlayerHook {
    static final String JS = "(function(){var vs=document.querySelectorAll('video'),b=null,bd=-1;"
        + "for(var i=0;i<vs.length;i++){var v=vs[i];var d=v.duration;if(d===Infinity)d=1e7;if(!isFinite(d))d=0;"
        + "if(!v.paused&&d>=bd){b=v;bd=d;}}"
        + "if(!b){for(var j=0;j<vs.length;j++){if(vs[j].readyState>0){b=vs[j];break;}}}"
        + "if(!b)return '';"
        + "return JSON.stringify({u:b.currentSrc||b.src||'',t:b.currentTime||0,n:document.title||''});})()";

    static void enter(final MainActivity a, final View view, final WebChromeClient.CustomViewCallback cb) {
        if (!Prefs.builtinPlayer(a)) { a.showFull(view, cb); return; }
        if (a.cur == null) { a.showFull(view, cb); return; }
        final WebView w = a.cur.wv;
        w.evaluateJavascript(JS, r -> {
            String u = "", n = "";
            double t = 0;
            try {
                Object o = new JSONTokener(r).nextValue();
                if (o instanceof String && !((String) o).isEmpty()) {
                    JSONObject j = new JSONObject((String) o);
                    u = j.optString("u");
                    t = j.optDouble("t", 0);
                    n = j.optString("n");
                }
            } catch (Exception e) { }
            final String bu = u.startsWith("http") ? u : a.bestVideoUrl(a.cur, u);
            if (bu != null) {
                w.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){try{v.pause()}catch(e){}})", null);
                cb.onCustomViewHidden();
                Intent i = new Intent(a, PlayerActivity.class);
                i.putExtra("url", bu);
                i.putExtra("name", n);
                i.putExtra("pos", t);
                i.putExtra("ua", w.getSettings().getUserAgentString());
                i.putExtra("page", w.getUrl());
                a.startActivity(i);
            } else {
                a.showFull(view, cb);
            }
        });
    }
}
