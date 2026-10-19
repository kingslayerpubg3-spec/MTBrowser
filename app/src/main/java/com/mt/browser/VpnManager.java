package com.mt.browser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class VpnManager {
    public interface Cb { void progress(String s); void done(boolean ok, String msg); }

    public static volatile boolean connected, busy, cancel, webOk;
    public static volatile String cc = "", host = "", ip = "", user, pass;
    public static volatile int port, ms;
    public static volatile long since;
    static final ProxySelector ORIG = ProxySelector.getDefault();
    static final Handler H = new Handler(Looper.getMainLooper());
    static final Executor DIRECT = new Executor() { public void execute(Runnable r) { r.run(); } };

    static final String RAW = "US:United States,GB:United Kingdom,CA:Canada,DE:Germany,FR:France,NL:Netherlands,IN:India,SG:Singapore,"
        + "JP:Japan,KR:South Korea,HK:Hong Kong,TW:Taiwan,CN:China,RU:Russia,UA:Ukraine,PL:Poland,IT:Italy,ES:Spain,PT:Portugal,"
        + "SE:Sweden,NO:Norway,FI:Finland,DK:Denmark,CH:Switzerland,AT:Austria,BE:Belgium,IE:Ireland,CZ:Czechia,RO:Romania,"
        + "BG:Bulgaria,HU:Hungary,GR:Greece,TR:Turkey,IL:Israel,AE:United Arab Emirates,SA:Saudi Arabia,QA:Qatar,KW:Kuwait,"
        + "IR:Iran,IQ:Iraq,PK:Pakistan,BD:Bangladesh,LK:Sri Lanka,NP:Nepal,ID:Indonesia,MY:Malaysia,TH:Thailand,VN:Vietnam,"
        + "PH:Philippines,KH:Cambodia,MM:Myanmar,KZ:Kazakhstan,UZ:Uzbekistan,AU:Australia,NZ:New Zealand,BR:Brazil,AR:Argentina,"
        + "CL:Chile,CO:Colombia,PE:Peru,VE:Venezuela,EC:Ecuador,MX:Mexico,ZA:South Africa,EG:Egypt,NG:Nigeria,KE:Kenya,"
        + "MA:Morocco,DZ:Algeria,TN:Tunisia,GH:Ghana,ET:Ethiopia,TZ:Tanzania,UG:Uganda,SN:Senegal,CM:Cameroon,LT:Lithuania,"
        + "LV:Latvia,EE:Estonia,RS:Serbia,HR:Croatia,SK:Slovakia,SI:Slovenia,BY:Belarus,MD:Moldova,GE:Georgia,AM:Armenia,"
        + "AZ:Azerbaijan,LU:Luxembourg,IS:Iceland,CY:Cyprus,MT:Malta,AL:Albania,BA:Bosnia and Herzegovina,MK:North Macedonia,"
        + "MN:Mongolia,AF:Afghanistan,JO:Jordan,LB:Lebanon,OM:Oman,BH:Bahrain,YE:Yemen,SY:Syria,DO:Dominican Republic,"
        + "CR:Costa Rica,PA:Panama,GT:Guatemala,HN:Honduras,BO:Bolivia,PY:Paraguay,UY:Uruguay,CU:Cuba,JM:Jamaica,PR:Puerto Rico,"
        + "TT:Trinidad and Tobago,ZM:Zambia,ZW:Zimbabwe,MZ:Mozambique,AO:Angola,CI:Ivory Coast,SD:Sudan,LY:Libya,RW:Rwanda,MU:Mauritius";
    public static final String[][] COUNTRIES;

    static {
        String[] p = RAW.split(",");
        COUNTRIES = new String[p.length][];
        for (int i = 0; i < p.length; i++) COUNTRIES[i] = p[i].split(":", 2);
    }

    public static String flag(String c) {
        if (c == null || c.length() != 2) return "\uD83C\uDF10";
        return new String(Character.toChars(0x1F1E6 + c.charAt(0) - 'A')) + new String(Character.toChars(0x1F1E6 + c.charAt(1) - 'A'));
    }

    public static String name(String c) {
        if (c == null || c.isEmpty()) return "Custom server";
        for (String[] x : COUNTRIES) if (x[0].equals(c)) return x[1];
        return c;
    }

    static void post(final Cb cb, final String s) {
        H.post(new Runnable() { public void run() { if (cb != null) cb.progress(s); } });
    }

    static void fin(final Cb cb, final boolean ok, final String m) {
        busy = false;
        H.post(new Runnable() { public void run() { if (cb != null) cb.done(ok, m); } });
    }

    static ArrayList<String> fetch(String c) throws IOException {
        String u = "https://cdn.jsdelivr.net/gh/proxifly/free-proxy-list@main/proxies/countries/" + c + "/data.txt";
        HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection(Proxy.NO_PROXY);
        h.setConnectTimeout(8000);
        h.setReadTimeout(10000);
        int code = h.getResponseCode();
        if (code != 200) throw new IOException(code == 404 ? "Is country ki list nahi hai" : "List nahi mili (HTTP " + code + ")");
        BufferedReader br = new BufferedReader(new InputStreamReader(h.getInputStream()));
        ArrayList<String> out = new ArrayList<>();
        String l;
        while ((l = br.readLine()) != null && out.size() < 400) {
            l = l.trim();
            if (l.isEmpty() || l.toLowerCase().startsWith("socks")) continue;
            String hp = l.contains("://") ? l.substring(l.indexOf("://") + 3) : l;
            if (hp.matches("[0-9a-zA-Z.\\-]+:[0-9]{2,5}")) out.add(hp);
        }
        br.close();
        h.disconnect();
        Collections.shuffle(out);
        return out;
    }

    static int test(String hp, int to) {
        try {
            String[] p = hp.split(":");
            Proxy px = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(p[0], Integer.parseInt(p[1])));
            long t0 = System.currentTimeMillis();
            HttpURLConnection c = (HttpURLConnection) new URL("https://www.gstatic.com/generate_204").openConnection(px);
            c.setConnectTimeout(to);
            c.setReadTimeout(to);
            c.setInstanceFollowRedirects(false);
            int code = c.getResponseCode();
            c.disconnect();
            return code == 204 ? (int) Math.max(1, System.currentTimeMillis() - t0) : -1;
        } catch (Throwable e) {
            return -1;
        }
    }

    static boolean checkIp() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.ipify.org").openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            String s = br.readLine();
            br.close();
            c.disconnect();
            if (s != null && s.trim().matches("[0-9a-fA-F:.]{3,45}")) { ip = s.trim(); return true; }
        } catch (Throwable e) { }
        return false;
    }

    static void webProxy(String h, int p) {
        try {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) { webOk = false; return; }
            ProxyConfig cfg = new ProxyConfig.Builder().addProxyRule(h + ":" + p)
                    .addBypassRule("localhost").addBypassRule("127.0.0.1").build();
            ProxyController.getInstance().setProxyOverride(cfg, DIRECT, new Runnable() { public void run() { } });
            webOk = true;
        } catch (Throwable e) { webOk = false; }
    }

    static void apply(Context c, String code, String h, int p, String u, String pw, int m) {
        cc = code;
        host = h;
        port = p;
        user = u;
        pass = pw;
        ms = m;
        ip = "";
        since = System.currentTimeMillis();
        final Proxy px = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(h, p));
        ProxySelector.setDefault(new ProxySelector() {
            public List<Proxy> select(URI uri) {
                String x = uri.getHost();
                if (x == null || x.equals("localhost") || x.startsWith("127.") || x.startsWith("10.") || x.startsWith("192.168."))
                    return Collections.singletonList(Proxy.NO_PROXY);
                return Collections.singletonList(px);
            }
            public void connectFailed(URI uri, SocketAddress sa, IOException e) { }
        });
        if (u != null) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY && user != null) return new PasswordAuthentication(user, pass.toCharArray());
                    return null;
                }
            });
        } else Authenticator.setDefault(null);
        webProxy(h, p);
        connected = true;
    }

    public static void disconnect() {
        connected = false;
        user = null;
        pass = null;
        ip = "";
        ProxySelector.setDefault(ORIG);
        Authenticator.setDefault(null);
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE))
                ProxyController.getInstance().clearProxyOverride(DIRECT, new Runnable() { public void run() { } });
        } catch (Throwable e) { }
    }

    public static synchronized void connectCountry(final Context c, final String code, final Cb cb) {
        if (busy) return;
        busy = true;
        cancel = false;
        new Thread(new Runnable() {
            public void run() {
                try {
                    post(cb, "Servers ki list la raha hai...");
                    ArrayList<String> l = fetch(code);
                    if (l.isEmpty()) { fin(cb, false, "Is country me abhi koi server nahi"); return; }
                    int n = Math.min(l.size(), 48);
                    post(cb, n + " servers test ho rahe hain...");
                    ExecutorService ex = Executors.newFixedThreadPool(16);
                    ExecutorCompletionService<String[]> cs = new ExecutorCompletionService<>(ex);
                    for (int i = 0; i < n; i++) {
                        final String hp = l.get(i);
                        cs.submit(new Callable<String[]>() { public String[] call() { return new String[]{hp, String.valueOf(test(hp, 6000))}; } });
                    }
                    String best = null;
                    int bm = 0;
                    long end = System.currentTimeMillis() + 30000;
                    for (int i = 0; i < n && !cancel; i++) {
                        Future<String[]> f = cs.poll(Math.max(100, end - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                        if (f == null) break;
                        String[] r = f.get();
                        int m = Integer.parseInt(r[1]);
                        if (m > 0) { best = r[0]; bm = m; break; }
                    }
                    ex.shutdownNow();
                    if (cancel) { fin(cb, false, "Cancel kiya"); return; }
                    if (best == null) { fin(cb, false, "Is country ke saare servers abhi band hain. Dusri country try karo."); return; }
                    String[] p = best.split(":");
                    apply(c, code, p[0], Integer.parseInt(p[1]), null, null, bm);
                    post(cb, "IP check ho raha hai...");
                    checkIp();
                    fin(cb, true, webOk ? "Connected" : "Connected, par is phone ka WebView proxy support nahi karta, isliye browser pages is route se nahi jayenge (downloads aur player jayenge)");
                } catch (Throwable e) {
                    fin(cb, false, e instanceof IOException ? String.valueOf(e.getMessage()) : "Error: " + e.getMessage());
                }
            }
        }).start();
    }

    public static synchronized void connectCustom(final Context c, String raw, final Cb cb) {
        if (busy) return;
        String s = raw.trim();
        if (s.contains("://")) s = s.substring(s.indexOf("://") + 3);
        String u = null, pw = null;
        int at = s.lastIndexOf('@');
        if (at > 0) {
            String cr = s.substring(0, at);
            s = s.substring(at + 1);
            int cl = cr.indexOf(':');
            if (cl > 0) { u = cr.substring(0, cl); pw = cr.substring(cl + 1); }
        }
        final String[] hp = s.split(":");
        if (hp.length != 2 || !hp[1].matches("[0-9]{2,5}")) {
            H.post(new Runnable() { public void run() { if (cb != null) cb.done(false, "Format galat. host:port likho"); } });
            return;
        }
        final String fu = u, fpw = pw;
        busy = true;
        cancel = false;
        new Thread(new Runnable() {
            public void run() {
                try {
                    post(cb, "Server check ho raha hai...");
                    long t0 = System.currentTimeMillis();
                    apply(c, "", hp[0], Integer.parseInt(hp[1]), fu, fpw, 0);
                    if (!checkIp()) { disconnect(); fin(cb, false, "Server respond nahi kar raha ya password galat hai"); return; }
                    ms = (int) (System.currentTimeMillis() - t0);
                    fin(cb, true, "Connected");
                } catch (Throwable e) {
                    disconnect();
                    fin(cb, false, "Error: " + e.getMessage());
                }
            }
        }).start();
    }
}
