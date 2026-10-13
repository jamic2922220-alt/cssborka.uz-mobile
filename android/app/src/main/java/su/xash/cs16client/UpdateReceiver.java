package su.xash.cs16client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Ilova yangilangandan keyin (MY_PACKAGE_REPLACED) ishga tushadi:
 * ilovani qayta ochishga urinadi va "Yangilandi - ochish uchun bosing" bildirishnomasini chiqaradi.
 */
public class UpdateReceiver extends BroadcastReceiver {
    private static final String CH = "bt_update";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null || !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        Intent open = new Intent(ctx, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // 1) Eski Android'larda ilova o'zi qayta ochiladi
        try {
            ctx.startActivity(open);
        } catch (Exception ignored) {
        }

        // 2) Yangi Android'larda bildirishnoma (bosilsa ilova ochiladi)
        try {
            String lang = ctx.getSharedPreferences("boosttop", Context.MODE_PRIVATE).getString("lang", "uz");
            String title = "ru".equals(lang) ? "Приложение обновлено ✅" : "en".equals(lang) ? "App updated ✅" : "Ilova yangilandi ✅";
            String text = "ru".equals(lang) ? "Нажмите, чтобы открыть CS 1.6" : "en".equals(lang) ? "Tap to open CS 1.6" : "CS 1.6 ni ochish uchun bosing";

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? 0x04000000 /* FLAG_IMMUTABLE */ : 0);
            PendingIntent pi = PendingIntent.getActivity(ctx, 1, open, fl);

            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(new NotificationChannel(CH, "Yangilanish", NotificationManager.IMPORTANCE_HIGH));
                b = new Notification.Builder(ctx, CH);
            } else {
                b = new Notification.Builder(ctx);
            }
            b.setSmallIcon(ctx.getApplicationInfo().icon)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(pi)
                    .setAutoCancel(true);
            nm.notify(1001, b.build());
        } catch (Exception ignored) {
        }
    }
}
