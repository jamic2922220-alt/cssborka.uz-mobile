package su.xash.cs16client;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Har ~3 soatda saytdagi mobile.json ni tekshiradi: yangi versiya chiqqan bo'lsa -
 * ovozli bildirishnoma "Yangi versiya chiqdi" (bosilsa ilova ochilib, yangilash oynasi chiqadi).
 * Har bir yangi build uchun bir marta.
 */
public class UpdCheckReceiver extends BroadcastReceiver {
    static final String CH = "xc_newver";
    private static final String CONFIG_URL = "http://mmb.boost-top.com/mobile.json";

    static void schedule(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? 0x04000000 /* FLAG_IMMUTABLE */ : 0);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 7, new Intent(ctx, UpdCheckReceiver.class), fl);
            long every = 3 * AlarmManager.INTERVAL_HOUR;
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + every, every, pi);
        } catch (Exception ignored) {
        }
    }

    static int myBuild(Context ctx) {
        try {
            InputStream in = ctx.getAssets().open("bt_build.txt");
            byte[] b = new byte[32];
            int n = in.read(b);
            in.close();
            return Integer.parseInt(new String(b, 0, Math.max(0, n), "UTF-8").trim());
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public void onReceive(final Context ctx, Intent intent) {
        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(CONFIG_URL + "?t=" + System.currentTimeMillis()).openConnection();
                    c.setConnectTimeout(10000);
                    c.setReadTimeout(10000);
                    InputStream in = c.getInputStream();
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int k;
                    while ((k = in.read(buf)) > 0) bo.write(buf, 0, k);
                    in.close();
                    c.disconnect();
                    int b = new JSONObject(bo.toString("UTF-8")).optInt("apk_build", 0);
                    if (b > myBuild(ctx)) notifyNew(ctx, b);
                } catch (Exception ignored) {
                } finally {
                    pr.finish();
                }
            }
        }).start();
    }

    static void notifyNew(Context ctx, int build) {
        SharedPreferences p = ctx.getSharedPreferences("boosttop", Context.MODE_PRIVATE);
        if (p.getInt("upd_notified", 0) >= build) return;
        try {
            String lang = p.getString("lang", "uz");
            String title = "ru".equals(lang) ? "Вышла новая версия X CLIENT!" : "en".equals(lang) ? "New X CLIENT version!" : "X CLIENT ning yangi versiyasi chiqdi!";
            String text = "ru".equals(lang) ? "Нажмите, чтобы обновить" : "en".equals(lang) ? "Tap to update" : "Yangilash uchun bosing";
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            Intent open = new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("upd", true);
            int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? 0x04000000 : 0);
            PendingIntent pi = PendingIntent.getActivity(ctx, 2, open, fl);
            Notification.Builder nb;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(CH, "Yangi versiya", NotificationManager.IMPORTANCE_HIGH);
                ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
                nb = new Notification.Builder(ctx, CH);
            } else {
                nb = new Notification.Builder(ctx).setDefaults(Notification.DEFAULT_ALL).setPriority(Notification.PRIORITY_HIGH);
            }
            nb.setSmallIcon(ctx.getApplicationInfo().icon)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(pi)
                    .setAutoCancel(true);
            nm.notify(1002, nb.build());
            p.edit().putInt("upd_notified", build).apply();
        } catch (Exception ignored) {
        }
    }
}
