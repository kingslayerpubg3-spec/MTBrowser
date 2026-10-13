package com.mt.browser;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.*;

public class DownloadService extends Service {
    Handler h = new Handler(Looper.getMainLooper());
    Runnable tick = new Runnable() {
        public void run() {
            int n = 0;
            double sp = 0;
            for (DownloadEngine.Item it : DownloadEngine.snapshot())
                if (it.running) { n++; sp += it.speed; }
            if (n == 0) { stopForeground(true); stopSelf(); return; }
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1, note(n, sp));
            h.postDelayed(this, 1000);
        }
    };

    Notification note(int n, double sp) {
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, DownloadsActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, "mt_dl")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("MT Browser downloads")
                .setContentText(n + " active • " + DownloadsActivity.speedText(sp))
                .setOngoing(true).setContentIntent(pi).build();
    }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        NotificationChannel ch = new NotificationChannel("mt_dl", "Downloads", NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        Notification n = note(1, 0);
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(1, n);
        h.removeCallbacks(tick);
        h.post(tick);
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        h.removeCallbacks(tick);
        super.onDestroy();
    }
}
