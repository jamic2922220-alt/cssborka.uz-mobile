package su.xash.cs16client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Yuklash (o'yin fayllari, ilova yangilanishi) paytida ilovani "tirik" saqlaydi:
 * ekran o'chsa ham yoki boshqa ilovaga o'tilsa ham yuklash to'xtamaydi.
 * Bildirishnomada foiz ko'rinib turadi. Yuklashning o'zi MainActivity dagi oqimda - bu xizmat
 * faqat jarayonni oldingi planda ushlab turadi (foreground service) va telefon/Wi-Fi ni uxlatmaydi.
 */
public class KeepAliveService extends Service {
    private static final String CH = "xc_dl";
    private static final int ID = 7701;
    private static PowerManager.WakeLock wake;
    private static WifiManager.WifiLock wifi;
    private static long lastNotify = 0;
    private static int lastPct = -2;

    public static void start(Context c, String title) {
        try {
            Intent i = new Intent(c, KeepAliveService.class).putExtra("t", title);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
            else c.startService(i);
        } catch (Throwable ignored) {
            // Android 12+ fonda boshlashni taqiqlasa ham yuklash o'zi davom etadi (ekran yoqiq bo'lsa)
        }
    }

    /** pct: 0..100, -1 = noma'lum. Tez-tez chaqirilsa ham bildirishnoma 1 soniyada bir yangilanadi. */
    public static void progress(Context c, String title, int pct, String text) {
        long now = System.currentTimeMillis();
        if (pct == lastPct || now - lastNotify < 1000) return;
        lastNotify = now; lastPct = pct;
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(ID, build(c, title, pct, text));
        } catch (Throwable ignored) {
        }
    }

    public static void stop(Context c) {
        lastPct = -2;
        try {
            c.stopService(new Intent(c, KeepAliveService.class));
        } catch (Throwable ignored) {
        }
    }

    private static Notification build(Context c, String title, int pct, String text) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "Yuklash", NotificationManager.IMPORTANCE_LOW);
                ch.setSound(null, null);
                nm.createNotificationChannel(ch);
            }
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        int fl = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        b.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title == null ? "X CLIENT" : title)
                .setContentText(text == null ? "" : text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(c, 0, open, fl))
                .setProgress(100, Math.max(0, pct), pct < 0);
        return b.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String t = intent != null ? intent.getStringExtra("t") : null;
        Notification n = build(this, t, -1, "");
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            else startForeground(ID, n);
        } catch (Throwable e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            if (wake == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "xclient:dl");
                wake.setReferenceCounted(false);
            }
            wake.acquire(60 * 60 * 1000L);   // ko'pi bilan 1 soat
            if (wifi == null) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    wifi = wm.createWifiLock(Build.VERSION.SDK_INT >= 29 ? WifiManager.WIFI_MODE_FULL_HIGH_PERF : WifiManager.WIFI_MODE_FULL, "xclient:dl");
                    wifi.setReferenceCounted(false);
                }
            }
            if (wifi != null) wifi.acquire();
        } catch (Throwable ignored) {
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (wake != null && wake.isHeld()) wake.release();
            if (wifi != null && wifi.isHeld()) wifi.release();
        } catch (Throwable ignored) {
        }
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
