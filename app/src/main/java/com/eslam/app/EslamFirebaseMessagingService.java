package com.eslam.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * خدمة استقبال إشعارات Firebase Cloud Messaging (FCM)
 * لتطبيق «الإسلام» - package: com.eslam.app
 */
public class EslamFirebaseMessagingService extends FirebaseMessagingService {

    public static final String TAG = "EslamFCM";
    public static final String CHANNEL_ID = "eslam_fcm_notifications_channel";
    public static final String CHANNEL_NAME = "تطبيق إسلام - الإشعارات والتنبيهات";
    public static final String PREFS_NAME = "eslam_fcm_prefs";
    public static final String KEY_FCM_TOKEN = "fcm_token";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "تم تجديد رمز جهاز FCM الجديد: " + token);
        
        // حفظ التوكن في SharedPreferences لاستخدامه عند الحاجة
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
        } catch (Exception e) {
            Log.e(TAG, "خطأ في حفظ توكن FCM: " + e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "تم استقبال إشعار FCM من: " + remoteMessage.getFrom());

        String title = "تطبيق إسلام";
        String body = "";
        String targetUrl = null;

        // 1. قراءة بيانات Notification إذا كانت متوفرة
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null && !remoteMessage.getNotification().getTitle().isEmpty()) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        // 2. قراءة بيانات Data Payload الإضافية
        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            if (data.containsKey("title") && data.get("title") != null && !data.get("title").isEmpty()) {
                title = data.get("title");
            }
            if (data.containsKey("body") && data.get("body") != null) {
                body = data.get("body");
            } else if (data.containsKey("message") && data.get("message") != null) {
                body = data.get("message");
            }

            // فحص الرابط المخصص للتوجيه (لفتح سورة معينة أو قارئ أو قسم معين)
            if (data.containsKey("url")) {
                targetUrl = data.get("url");
            } else if (data.containsKey("link")) {
                targetUrl = data.get("link");
            } else if (data.containsKey("targetUrl")) {
                targetUrl = data.get("targetUrl");
            }
        }

        sendNotification(title, body, targetUrl);
    }

    private void sendNotification(String title, String messageBody, String targetUrl) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (targetUrl != null && !targetUrl.trim().isEmpty()) {
            intent.putExtra("url", targetUrl.trim());
        }

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) { // Build.VERSION_CODES.M
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                pendingFlags
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Bitmap largeIcon = null;
        try {
            largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        } catch (Exception ignored) {}

        // إنشاء قناة الإشعارات لنظام أندرويد 8.0 فما فوق
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26 && notificationManager != null) { // Build.VERSION_CODES.O
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("قناة إشعارات تطبيق إسلام للأذكار، التنبيهات، والآيات القرآنية");
            channel.enableVibration(true);
            channel.enableLights(true);
            notificationManager.createNotificationChannel(channel);
        }

        if (notificationManager != null) {
            android.app.Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= 26) {
                builder = new android.app.Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new android.app.Notification.Builder(this);
            }

            builder.setSmallIcon(R.mipmap.ic_launcher)
                   .setContentTitle(title)
                   .setContentText(messageBody)
                   .setAutoCancel(true)
                   .setSound(defaultSoundUri)
                   .setContentIntent(pendingIntent);

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon);
            }

            if (messageBody != null && messageBody.length() > 40 && Build.VERSION.SDK_INT >= 16) {
                builder.setStyle(new android.app.Notification.BigTextStyle().bigText(messageBody));
            }

            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
        }
    }
}
