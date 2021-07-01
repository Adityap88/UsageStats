package com.research.usage_stats;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MyNotificationManager {

    private Context context;
    private static MyNotificationManager instance;
    private NotificationManagerCompat notificationManagerCompat;
    private NotificationManager notificationManager;

    private MyNotificationManager(Context context){
        this.context = context;
        notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static MyNotificationManager getInstance(Context context){
        if(instance==null){
            instance = new MyNotificationManager(context);
        }
        return instance;
    }

    public void registerNotificationChannelChannel(String channelId, String channelName, String channelDescription) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription(channelDescription);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public Notification getNotification(Class targetNotificationActivity, String title,  int priority, boolean autoCancel, int notificationId){
        Intent intent = new Intent(context, targetNotificationActivity);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,"123")
                .setSmallIcon(R.drawable.back)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setChannelId("123")
                .setAutoCancel(autoCancel);
        return builder.build();

    }

    public void cancelNotification(int notificationId){
        notificationManager.cancel(notificationId);
    }
}